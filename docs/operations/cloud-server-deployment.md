# ERP 云服务器部署指南

本文按单台 Ubuntu 22.04/24.04 云服务器部署设计：Nginx 提供前端和 HTTPS，Spring Boot 运行后端，MySQL 8 保存业务数据，Redis 提供缓存与业务单号序列。

## 1. 服务器准备

建议最低配置为 4 核 CPU、8 GB 内存、100 GB SSD。生产环境开放 `22`、`80`、`443`，不要向公网开放 MySQL `3306`、Redis `6379` 和后端 `8080`。

安装基础软件：

```bash
sudo apt update
sudo apt install -y git nginx mysql-server redis-server maven curl unzip
```

安装 Temurin JDK 8 和 Node.js 20。确认版本：

```bash
java -version
node -v
npm -v
```

安装 pnpm：

```bash
sudo npm install -g pnpm@8.15.9
```

## 2. 拉取代码

```bash
sudo mkdir -p /opt/hyldsys
sudo chown "$USER":"$USER" /opt/hyldsys
git clone git@github.com:Curosity651/erp.git /opt/hyldsys/app
cd /opt/hyldsys/app
git checkout master
```

服务器需要配置只读 Deploy Key，或者使用有仓库读取权限的 GitHub SSH key。

## 3. MySQL 与 Redis

创建生产数据库和最小权限账户：

```sql
CREATE DATABASE erp CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'erp'@'127.0.0.1' IDENTIFIED BY '替换为高强度密码';
GRANT ALL PRIVILEGES ON erp.* TO 'erp'@'127.0.0.1';
FLUSH PRIVILEGES;
```

本项目当前不自动执行 Flyway。首次部署应把本地已经验证过的数据库通过私密通道传到服务器，再导入；不要把数据库备份提交到 Git：

```bash
mysql -u root -p erp < /secure/path/erp-production.sql
```

如果部署的是空白基线库，则必须按版本顺序执行 `erp-backend/sql/migration` 中尚未应用的迁移。每次升级前先做备份，并记录最后已执行版本。当前代码要求至少执行到 `V138`。

Redis 仅监听本机，并设置密码：

```bash
sudo nano /etc/redis/redis.conf
# bind 127.0.0.1 ::1
# requirepass 替换为高强度密码
sudo systemctl restart redis-server
```

## 4. 构建前后端

后端：

```bash
cd /opt/hyldsys/app/erp-backend
mvn -pl admin clean package -DskipTests
sudo mkdir -p /opt/hyldsys/backend
sudo cp admin/target/admin-2.0.0-SNAPSHOT.jar /opt/hyldsys/backend/app.jar
```

前端：

```bash
cd /opt/hyldsys/app/erp-frontend
pnpm install --frozen-lockfile
pnpm build
sudo mkdir -p /var/www/hyldsys
sudo rsync -a --delete dist/ /var/www/hyldsys/
```

## 5. 后端环境变量

创建 `/etc/hyldsys/backend.env`，权限必须限制为 root 可读：

```bash
sudo mkdir -p /etc/hyldsys
sudo nano /etc/hyldsys/backend.env
sudo chmod 600 /etc/hyldsys/backend.env
```

示例内容：

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/erp?rewriteBatchedStatements=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true
DB_USERNAME=erp
DB_PASSWORD=替换为数据库密码
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=替换为Redis密码
OSS_ENDPOINT=oss-cn-shanghai.aliyuncs.com
OSS_ACCESS_KEY_ID=替换为OSS密钥ID
OSS_ACCESS_KEY_SECRET=替换为OSS密钥
OSS_BUCKET_NAME=替换为存储桶名称
OSS_CDN_DOMAIN=https://你的文件域名
FIXER_API_KEYS=key1,key2
```

## 6. Systemd 后端服务

创建 `/etc/systemd/system/hyldsys-backend.service`：

```ini
[Unit]
Description=HYLDsys ERP Backend
After=network.target mysql.service redis-server.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/hyldsys/backend
EnvironmentFile=/etc/hyldsys/backend.env
ExecStart=/usr/bin/java -Xms1g -Xmx4g -jar /opt/hyldsys/backend/app.jar --spring.profiles.active=prod
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

启动并检查：

```bash
sudo chown -R www-data:www-data /opt/hyldsys/backend
sudo systemctl daemon-reload
sudo systemctl enable --now hyldsys-backend
sudo systemctl status hyldsys-backend
sudo journalctl -u hyldsys-backend -f
```

## 7. Nginx

创建 `/etc/nginx/sites-available/hyldsys`：

```nginx
server {
    listen 80;
    server_name erp.example.com;

    root /var/www/hyldsys;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        client_max_body_size 100m;
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/hyldsys /etc/nginx/sites-enabled/hyldsys
sudo nginx -t
sudo systemctl reload nginx
```

配置域名解析后，使用 Certbot 开启 HTTPS：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d erp.example.com
```

## 8. 发布与回滚

每次发布前执行：

```bash
git pull --ff-only origin master
cd erp-backend && mvn -pl admin test
cd ../erp-frontend && pnpm type-check && pnpm exec vitest run && pnpm build
```

备份数据库和旧 JAR 后，再复制新构建产物并重启：

```bash
sudo cp /opt/hyldsys/backend/app.jar /opt/hyldsys/backend/app.jar.bak
sudo cp erp-backend/admin/target/admin-2.0.0-SNAPSHOT.jar /opt/hyldsys/backend/app.jar
sudo rsync -a --delete erp-frontend/dist/ /var/www/hyldsys/
sudo systemctl restart hyldsys-backend
sudo systemctl reload nginx
```

若启动失败，恢复 `app.jar.bak` 和对应数据库备份。数据库迁移必须先在测试环境验证，不可在生产库直接试错。

## 9. 上线检查

- `systemctl status hyldsys-backend` 为 active。
- `curl -I http://127.0.0.1:8080/login` 能连接后端，未登录返回 401 属于正常权限行为。
- 浏览器访问域名可进入登录页，刷新任意路由不会 404。
- 上传文件、登录、库存查询、收货上架、订单下架和拣货签出至少各做一次冒烟测试。
- MySQL、Redis、8080 不对公网开放；数据库和 OSS 密钥不进入 Git。
