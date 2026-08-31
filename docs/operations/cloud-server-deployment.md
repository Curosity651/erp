# ERP 云服务器部署指南

本文按单台 Ubuntu 22.04/24.04 云服务器部署设计：Nginx 提供前端和 HTTPS，Spring Boot 运行后端，MySQL 8 保存业务数据，Redis 提供缓存与业务单号序列。

当前部署基线：

- 分支：`master`
- 业务代码验证基线：`f1d4e60`
- 数据库迁移：至少 `V139__finish_logical_location_menu_cutover.sql`
- WMS 库存内核：`LOGICAL_LOCATION`
- 生产端口：Nginx `80/443`，后端仅本机 `8080`

本次基线已经彻底切换为逻辑库位库存。生产环境不得重新启用旧几何库位、托盘或托位写入流程。

## 1. 服务器准备

建议最低配置为 4 核 CPU、8 GB 内存、100 GB SSD。生产环境开放 `22`、`80`、`443`，不要向公网开放 MySQL `3306`、Redis `6379` 和后端 `8080`。

安装基础软件：

```bash
sudo apt update
sudo apt install -y git nginx mysql-server redis-server maven curl unzip rsync
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
git pull --ff-only origin master
git rev-parse HEAD
```

`git rev-parse HEAD` 应显示准备发布的 `master` 提交；本轮业务代码基线为 `f1d4e600a193b090b19262f577a366c4ca8d801e`，后续可能包含文档或部署修订提交。服务器需要配置只读 Deploy Key，或者使用有仓库读取权限的 GitHub SSH key。

## 3. MySQL 与 Redis

创建生产数据库和最小权限账户：

```sql
CREATE DATABASE erp CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'erp'@'127.0.0.1' IDENTIFIED BY '替换为高强度密码';
GRANT ALL PRIVILEGES ON erp.* TO 'erp'@'127.0.0.1';
FLUSH PRIVILEGES;
```

本项目当前不自动执行 Flyway。首次部署不能只创建空数据库后直接启动应用，必须先把已经验证过的完整数据库基线通过私密通道传到服务器并导入；不要把数据库备份提交到 Git：

```bash
mysql -u root -p erp < /secure/path/erp-production.sql
```

`erp-backend/sql/migration` 是增量迁移集合，不是完整初始化库。只有已经具备项目基础表结构的数据库，才能按数值版本顺序补执行尚未应用的迁移。每次升级前必须备份数据库，并在发布记录中写明最后已执行版本。当前代码要求至少执行到 `V139`。

从上一版 `V138` 升级到本次基线时执行：

```bash
mkdir -p /opt/hyldsys/backups
mysqldump -u root -p --single-transaction --routines --triggers erp \
  > /opt/hyldsys/backups/erp-before-v139-$(date +%Y%m%d-%H%M%S).sql

mysql -u root -p erp \
  < /opt/hyldsys/app/erp-backend/sql/migration/V139__finish_logical_location_menu_cutover.sql
```

迁移后校验：

```bash
mysql -u root -p erp -e "
SELECT id,title,path,uri,hidden
FROM sys_menu
WHERE id IN (162004,170505,180400);
"
```

应满足：`170505` 指向 `platform/return-ops/ReturnQcPage`，`180400` 指向 `wms/storage-overview/index`，`162004.hidden=1`。

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
mvn -pl admin clean test
mvn -pl admin package -DskipTests
sudo mkdir -p /opt/hyldsys/backend
sudo cp admin/target/admin-2.0.0-SNAPSHOT.jar /opt/hyldsys/backend/app.jar
```

前端：

```bash
cd /opt/hyldsys/app/erp-frontend
pnpm install --frozen-lockfile
pnpm exec vitest run
pnpm run build
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

不要把真实密码、OSS 密钥或数据库备份写入仓库。`DB_PASSWORD` 必填；使用上传、合同 PDF、付款凭证和作业照片功能时，OSS 配置也必须完整有效。

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
ExecStart=/usr/bin/java -Xms1g -Xmx4g -Duser.timezone=Asia/Shanghai -jar /opt/hyldsys/backend/app.jar --spring.profiles.active=prod
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

确认后端只监听本机或通过云防火墙禁止公网访问 `8080`。未登录访问业务接口返回 `401` 是正常现象。

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
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
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

### 8.1 发布前检查

```bash
cd /opt/hyldsys/app
git pull --ff-only origin master
git status --short
git rev-parse HEAD

cd erp-backend
mvn test
mvn -pl admin package -DskipTests

cd ../erp-frontend
pnpm install --frozen-lockfile
pnpm exec vitest run
pnpm run build
```

`git status --short` 必须无输出。数据库迁移应在停止旧服务前完成备份，并在复制新 JAR 前执行。

### 8.2 发布

```bash
cd /opt/hyldsys/app

sudo cp /opt/hyldsys/backend/app.jar /opt/hyldsys/backend/app.jar.bak
sudo cp erp-backend/admin/target/admin-2.0.0-SNAPSHOT.jar /opt/hyldsys/backend/app.jar
sudo rsync -a --delete erp-frontend/dist/ /var/www/hyldsys/
sudo systemctl restart hyldsys-backend
sudo systemctl reload nginx

sudo systemctl --no-pager --full status hyldsys-backend
curl -fsS http://127.0.0.1:8080/actuator/health
```

### 8.3 回滚

若新版本启动失败：

```bash
sudo systemctl stop hyldsys-backend
sudo cp /opt/hyldsys/backend/app.jar.bak /opt/hyldsys/backend/app.jar
sudo systemctl start hyldsys-backend
sudo journalctl -u hyldsys-backend -n 200 --no-pager
```

只有迁移改变导致旧代码无法兼容时才恢复对应数据库备份。恢复数据库前必须停止后端，并额外备份当前故障现场。数据库迁移必须先在测试环境验证，不可在生产库直接试错。

## 9. 上线检查

- `systemctl status hyldsys-backend` 为 active。
- `curl http://127.0.0.1:8080/actuator/health` 返回 `UP`。
- `curl -I http://127.0.0.1:8080/login` 能连接后端；未登录业务请求返回 401 属于正常权限行为。
- 浏览器访问域名可进入登录页，刷新任意路由不会 404。
- ERP、WMS 服务商、海外仓平台三个身份均能登录，菜单没有乱码或空白页。
- 库位管理只展示逻辑库位；可以按排新增库位，旧结构生成和托盘规则入口不再出现。
- 退货质检和仓储概览可以直接打开；自定义退货单入口不显示。
- 新建入库单时，缺少外箱尺寸或单箱毛重的 SKU 会被明确拦截。
- 上传文件、库存查询、收货上架、订单下架、拣货、打印面单和批量签出至少各做一次冒烟测试。
- MySQL、Redis、8080 不对公网开放；数据库和 OSS 密钥不进入 Git。

## 10. 常用运维命令

```bash
# 查看当前部署提交
cd /opt/hyldsys/app && git rev-parse --short HEAD

# 查看后端状态与最近日志
sudo systemctl status hyldsys-backend
sudo journalctl -u hyldsys-backend -n 200 --no-pager

# 重启后端
sudo systemctl restart hyldsys-backend

# 检查 Nginx 配置并重载
sudo nginx -t && sudo systemctl reload nginx

# 检查监听端口
sudo ss -lntp | grep -E ':80|:443|:8080|:3306|:6379'
```
