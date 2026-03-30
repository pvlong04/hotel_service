# Production Hardening Checklist

## 1) Secrets and credentials

- Dat `DB_USERNAME` va `DB_PASSWORD` trong environment.
- Dat `JWT_SIGNER_KEY` manh (>= 64 ky tu ngau nhien).
- Khong commit secret vao git.

## 2) Bootstrap admin

- Mac dinh da tat: `BOOTSTRAP_ADMIN_ENABLED=false`.
- Neu can tao admin ban dau:
  - Bat `BOOTSTRAP_ADMIN_ENABLED=true`
  - Dat `BOOTSTRAP_ADMIN_PASSWORD` manh (>= 8)
  - Co the dat them `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL`
- Sau khi tao xong, tat bootstrap lai.

## 3) Tests and CI

- Chay unit tests moi lan truoc merge.
- Bo sung integration tests cho auth, users, hotels, rooms, reservations.
- Dat gate tren CI: build + test pass moi duoc deploy.

## 4) API docs

- Dong bo Postman collection voi endpoint moi (`/reservations`).
- Cap nhat tai lieu luong booking khi thay doi business rule.

