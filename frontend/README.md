# AIoT Log Frontend

Vue 3 frontend for the AIoT intelligent device running log management system.

## Tech Stack

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios

## Run

Install dependencies:

```powershell
cd D:\AI\IOT\frontend
npm install
```

Start the development server:

```powershell
npm run dev
```

Local URL:

```text
http://127.0.0.1:5173/
```

The Vite dev server proxies `/api` to:

```text
http://127.0.0.1:8080
```

Make sure the backend and MySQL are running before using the pages.

## Build

```powershell
npm run build
```

Current build result:

```text
vue-tsc --noEmit --incremental false && vite build
BUILD SUCCESS
```

## Implemented Pages

```text
/dashboard   首页统计
/devices     设备管理
/devices/:id 设备详情
/logs        日志管理
/tags        标签管理
```

## Implemented API Modules

```text
src/api/dashboard.ts
src/api/devices.ts
src/api/logs.ts
src/api/tags.ts
src/api/enums.ts
```
