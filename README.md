## Pharmacy Backend Overview
# Tổng quan

Backend của dự án Pharmacy App được thiết kế theo kiến trúc microservices, sử dụng Spring Boot + Spring Cloud, giao tiếp qua API Gateway, và tách riêng nhóm service cho user side và admin side. Hệ thống hướng tới khả năng mở rộng, triển khai độc lập từng service, và xử lý nghiệp vụ theo domain.

# Backend làm được gì
Xác thực và quản lý danh tính người dùng qua identity-service (JWT, bảo mật liên service).
Quản lý thông tin người dùng qua user-service.
Cung cấp danh mục sản phẩm, thông tin thuốc, nội dung mô tả qua catalog-service và content-service.
Quản lý chi nhánh nhà thuốc qua branch-service.
Quản lý tồn kho theo chi nhánh qua inventory-service.
Hỗ trợ giỏ hàng và cập nhật giỏ hàng theo thời gian sử dụng qua cart-service.
Xử lý đặt hàng, lưu đơn hàng, theo dõi trạng thái qua order-service.
Xử lý thanh toán qua payment-service.
Quản lý mã giảm giá/chương trình khuyến mãi qua discount-service.
Đặt lịch khám/tư vấn qua appointment-service.
Quản lý dược sĩ và các nghiệp vụ liên quan qua pharmacist-service.
Đánh giá, nhận xét sản phẩm/dịch vụ qua review-service.
Gửi thông báo theo sự kiện hệ thống qua notification-service.
Quản lý upload media (ảnh/tài liệu) qua media-service.
Hỗ trợ nghiệp vụ AI service trong hệ thống qua ai-service.
Cung cấp một lớp tổng hợp cho admin qua admin-bff-service.
Quản trị nội dung qua cms-service.
Tổng hợp báo cáo, chỉ số vận hành qua reporting-service.
Lưu vết hành động quản trị qua audit-service.
Quản lý cấu hình hệ thống qua settings-service.
# Khả năng kỹ thuật
API Gateway là điểm vào duy nhất cho client web/mobile.
Config Server tập trung cấu hình cho các service.
Mỗi service có DB/schema riêng, giảm coupling và dễ scale độc lập.
Event-driven communication qua Kafka cho các luồng bất đồng bộ.
Hỗ trợ object storage (S3/MinIO) cho media.
Maven multi-module giúp build đồng bộ toàn bộ hệ thống.
Có sẵn CI/CD workflow cho frontend và backend trong GitHub Actions.
# Thành phần chia sẻ
common-model: model dùng chung.
common-security: xử lý bảo mật dùng chung.
common-messaging: quy ước message/event dùng chung.
# Vận hành và triển khai
Có hướng dẫn kiến trúc tổng quan trong ARCHITECTURE.md.
Có hướng dẫn deploy Render trong README-Render.md.
Hỗ trợ chạy local qua script và docker-compose để khởi động nhanh.
# Giá trị chính của backend
Bao phủ đầy đủ nghiệp vụ cốt lõi của ứng dụng nhà thuốc (user + admin).
Dễ mở rộng theo domain mới mà không ảnh hưởng lớn đến service hiện tại.
Dễ tích hợp hệ thống ngoài nhờ event và API tách biệt rõ ràng.
Thuận tiện cho vận hành production nhờ phân tách service, quản lý cấu hình tập trung và deployment theo môi trường.
