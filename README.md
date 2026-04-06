# Pharmacy Backend Overview

## Tong quan

Backend cua du an Pharmacy App duoc thiet ke theo kien truc microservices, su dung Spring Boot + Spring Cloud, giao tiep qua API Gateway, va tach rieng nhom service cho user side va admin side. He thong huong toi kha nang mo rong, trien khai doc lap tung service, va xu ly nghiep vu theo domain.

## Backend lam duoc gi

- Xac thuc va quan ly danh tinh nguoi dung qua `identity-service` (JWT, bao mat lien service).
- Quan ly thong tin nguoi dung qua `user-service`.
- Cung cap danh muc san pham, thong tin thuoc, noi dung mo ta qua `catalog-service` va `content-service`.
- Quan ly chi nhanh nha thuoc qua `branch-service`.
- Quan ly ton kho theo chi nhanh qua `inventory-service`.
- Ho tro gio hang va cap nhat gio hang theo thoi gian su dung qua `cart-service`.
- Xu ly dat hang, luu don hang, theo doi trang thai qua `order-service`.
- Xu ly thanh toan qua `payment-service`.
- Quan ly ma giam gia/chuong trinh khuyen mai qua `discount-service`.
- Dat lich kham/tu van qua `appointment-service`.
- Quan ly duoc si va cac nghiep vu lien quan qua `pharmacist-service`.
- Danh gia, nhan xet san pham/dich vu qua `review-service`.
- Gui thong bao theo su kien he thong qua `notification-service`.
- Quan ly upload media (anh/tai lieu) qua `media-service`.
- Ho tro nghiep vu AI service trong he thong qua `ai-service`.
- Cung cap mot lop tong hop cho admin qua `admin-bff-service`.
- Quan tri noi dung qua `cms-service`.
- Tong hop bao cao, chi so van hanh qua `reporting-service`.
- Luu vet hanh dong quan tri qua `audit-service`.
- Quan ly cau hinh he thong qua `settings-service`.

## Kha nang ky thuat

- API Gateway la diem vao duy nhat cho client web/mobile.
- Config Server tap trung cau hinh cho cac service.
- Moi service co DB/schema rieng, giam coupling va de scale doc lap.
- Event-driven communication qua Kafka cho cac luong bat dong bo.
- Ho tro object storage (S3/MinIO) cho media.
- Maven multi-module giup build dong bo toan bo he thong.
- Co san CI/CD workflow cho frontend va backend trong GitHub Actions.

## Thanh phan chia se

- `common-model`: model dung chung.
- `common-security`: xu ly bao mat dung chung.
- `common-messaging`: quy uoc message/event dung chung.

## Van hanh va trien khai

- Co huong dan kien truc tong quan trong `ARCHITECTURE.md`.
- Co huong dan deploy Render trong `README-Render.md`.
- Ho tro chay local qua script va docker-compose de khoi dong nhanh.

## Gia tri chinh cua backend

- Bao phu day du nghiep vu cot loi cua ung dung nha thuoc (user + admin).
- De mo rong theo domain moi ma khong anh huong lon den service hien tai.
- De tich hop he thong ngoai nho event va API tach biet ro rang.
- Thuan tien cho van hanh production nho phan tach service, quan ly cau hinh tap trung va deployment theo moi truong.
