-- ============================================
-- Database Schema cho MedCard System (v2)
-- MySQL Database
-- ============================================

-- Tạo database nếu chưa có
CREATE DATABASE IF NOT EXISTS medcard CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medcard;

-- ============================================
-- 1. Bảng thông tin nhân viên (Staff Info)
--   - Lưu toàn bộ nhân sự liên quan: bác sĩ, dược sĩ, nhân viên kho, admin...
--   - Thẻ sẽ chỉ mang staff_id + role, hệ thống mở rộng chi tiết ở đây.
-- ============================================
CREATE TABLE IF NOT EXISTS staff_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id VARCHAR(20) UNIQUE NOT NULL COMMENT 'Mã nhân viên (unique, dùng trên thẻ)',
    short_name VARCHAR(100) NOT NULL COMMENT 'Tên rút gọn / tên hiển thị',
    full_name VARCHAR(200) COMMENT 'Họ tên đầy đủ',
    role TINYINT NOT NULL COMMENT '0=Admin, 1=Dược sĩ, 2=Nhân viên kho',
    department VARCHAR(100) COMMENT 'Phòng ban / bộ phận',
    phone VARCHAR(20) COMMENT 'Số điện thoại',
    email VARCHAR(150) COMMENT 'Email công việc',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Trạng thái: 1=Hoạt động, 0=Khoá',
    last_login_at DATETIME COMMENT 'Lần đăng nhập gần nhất',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    INDEX idx_staff_id (staff_id),
    INDEX idx_role (role),
    INDEX idx_department (department),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Thông tin nhân viên';

-- ============================================
-- 1.1. Bảng tài khoản admin đăng nhập bằng mật khẩu (Admin Accounts)
--   - Map 1-1 với staff_info thông qua staff_id
--   - Chỉ dùng cho các tài khoản admin không dùng thẻ
-- ============================================
CREATE TABLE IF NOT EXISTS admin_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id VARCHAR(20) NOT NULL COMMENT 'FK -> staff_info.staff_id',
    username VARCHAR(100) UNIQUE NOT NULL COMMENT 'Tên đăng nhập',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Mật khẩu đã hash (BCrypt/Argon2...)',
    active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=Hoạt động, 0=Khoá',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    FOREIGN KEY (staff_id) REFERENCES staff_info(staff_id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY uq_admin_staff (staff_id),
    INDEX idx_admin_username (username),
    INDEX idx_admin_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tài khoản admin đăng nhập bằng mật khẩu';

-- ============================================
-- 2. Bảng thông tin thuốc (Medicines - master)
--   - Thông tin chung của thuốc (mã, tên, đơn vị...)
--   - Tồn kho thực tế sẽ quản lý chi tiết theo lô ở bảng medicine_batches.
--   - Cột quantity ở đây có thể dùng làm tổng tồn (cache) nếu muốn.
-- ============================================
CREATE TABLE IF NOT EXISTS medicines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL COMMENT 'Mã thuốc (unique)',
    name VARCHAR(200) NOT NULL COMMENT 'Tên thuốc',
    unit VARCHAR(50) DEFAULT 'viên' COMMENT 'Đơn vị (viên, lọ, hộp...)',
    quantity INT NOT NULL DEFAULT 0 COMMENT 'Tổng số lượng còn lại (có thể sync từ medicine_batches)',
    min_quantity INT NOT NULL DEFAULT 0 COMMENT 'Ngưỡng cảnh báo sắp hết',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    INDEX idx_code (code),
    INDEX idx_name (name),
    INDEX idx_quantity (quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Thông tin thuốc (master)';

-- ============================================
-- 3. Bảng lô thuốc (Medicine Batches)
--   - Phân biệt cùng thuốc nhưng khác lô, ngày nhập, hạn dùng, vị trí kho...
-- ============================================
CREATE TABLE IF NOT EXISTS medicine_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_code VARCHAR(20) NOT NULL COMMENT 'Mã thuốc (FK -> medicines.code)',
    batch_number VARCHAR(50) NOT NULL COMMENT 'Mã lô',
    expiry_date DATE COMMENT 'Hạn dùng',
    import_date DATE COMMENT 'Ngày nhập',
    quantity INT NOT NULL DEFAULT 0 COMMENT 'Số lượng tồn trong lô này',
    location VARCHAR(100) COMMENT 'Vị trí trong kho (kệ/ngăn...)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    FOREIGN KEY (medicine_code) REFERENCES medicines(code) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY uq_medicine_batch (medicine_code, batch_number, import_date),
    INDEX idx_medicine_code (medicine_code),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_quantity (quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tồn kho chi tiết theo lô thuốc';

-- ============================================
-- 4. Bảng đơn thuốc (Prescriptions - header)
--   - Lưu đơn thuốc do bác sĩ kê, dược sĩ xử lý.
--   - Log của đơn thuốc sẽ liên kết sang inventory_logs.
-- ============================================
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_code VARCHAR(30) UNIQUE NOT NULL COMMENT 'Mã đơn thuốc (hiển thị)',
    patient_id VARCHAR(50) NOT NULL COMMENT 'Mã bệnh nhân (tham chiếu hệ thống HIS ngoài)',
    doctor_staff_id VARCHAR(20) NOT NULL COMMENT 'Mã bác sĩ kê đơn (FK staff_info)',
    pharmacist_staff_id VARCHAR(20) COMMENT 'Mã dược sĩ xử lý đơn (FK staff_info)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=Mới,1=Đang xử lý,2=Hoàn tất,3=Hủy',
    note TEXT COMMENT 'Ghi chú',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo đơn',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    FOREIGN KEY (doctor_staff_id) REFERENCES staff_info(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (pharmacist_staff_id) REFERENCES staff_info(staff_id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_prescription_code (prescription_code),
    INDEX idx_patient_id (patient_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Đơn thuốc';

-- ============================================
-- 5. Bảng chi tiết đơn thuốc (Prescription Items)
-- ============================================
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL COMMENT 'FK -> prescriptions.id',
    medicine_code VARCHAR(20) NOT NULL COMMENT 'Mã thuốc',
    batch_id BIGINT COMMENT 'Lô thuốc xuất (nếu đã gán cụ thể lô)',
    quantity INT NOT NULL COMMENT 'Số lượng kê',
    dosage VARCHAR(100) COMMENT 'Liều dùng (ví dụ: 2 viên x 3 lần/ngày)',
    note TEXT COMMENT 'Ghi chú chi tiết',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (medicine_code) REFERENCES medicines(code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES medicine_batches(id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_medicine_code (medicine_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chi tiết đơn thuốc';

-- ============================================
-- 6. Bảng log xuất-nhập kho (Inventory Logs)
--   - Ghi lại mọi thay đổi tồn kho: nhập kho, xuất kho, điều chỉnh, xuất theo đơn...
--   - Liên kết với lô, thuốc, nhân viên và (nếu có) đơn thuốc.
-- ============================================
CREATE TABLE IF NOT EXISTS inventory_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL COMMENT 'Thời gian thao tác',
    type TINYINT NOT NULL COMMENT '0=Nhập kho,1=Xuất kho,2=Điều chỉnh,3=Huỷ thuốc',
    medicine_code VARCHAR(20) NOT NULL COMMENT 'Mã thuốc',
    batch_id BIGINT COMMENT 'Lô thuốc liên quan',
    quantity_change INT NOT NULL COMMENT 'Số lượng thay đổi (+ nhập, - xuất)',
    staff_id VARCHAR(20) NOT NULL COMMENT 'Mã nhân viên thực hiện',
    prescription_id BIGINT COMMENT 'Đơn thuốc liên quan (nếu là xuất theo đơn)',
    ref_type VARCHAR(30) COMMENT 'Loại tham chiếu: PRESCRIPTION, MANUAL, ADJUST, AUDIT...',
    ref_id VARCHAR(50) COMMENT 'Mã tham chiếu ngoài (mã phiếu, mã biên bản...)',
    note TEXT COMMENT 'Ghi chú chi tiết',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo record',
    FOREIGN KEY (medicine_code) REFERENCES medicines(code) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES medicine_batches(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES staff_info(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_timestamp (timestamp),
    INDEX idx_medicine_code (medicine_code),
    INDEX idx_staff_id (staff_id),
    INDEX idx_type (type),
    INDEX idx_prescription_id (prescription_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Log xuất-nhập kho chi tiết';

-- ============================================
-- 7. Bảng lịch sử kiểm tra kho (Audit History)
--   - Ghi lại các lần kiểm kê kho định kỳ.
-- ============================================
CREATE TABLE IF NOT EXISTS audit_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL COMMENT 'Thời gian kiểm tra',
    staff_id VARCHAR(20) NOT NULL COMMENT 'Mã nhân viên kiểm tra',
    medicine_count INT NOT NULL DEFAULT 0 COMMENT 'Số thuốc đã kiểm tra',
    result TINYINT NOT NULL DEFAULT 0 COMMENT 'Kết quả: 0=OK,1=Có lệch,2=Cảnh báo',
    note TEXT COMMENT 'Ghi chú',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo record',
    FOREIGN KEY (staff_id) REFERENCES staff_info(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_timestamp (timestamp),
    INDEX idx_staff_id (staff_id),
    INDEX idx_result (result),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lịch sử kiểm tra kho';

-- ============================================
-- 8. Bảng log hành vi hệ thống (System Action Logs)
--   - Ghi lại mọi hành vi query/update/operation của người dùng/hệ thống.
--   - Chỉ hệ thống / admin mới xem được.
-- ============================================
CREATE TABLE IF NOT EXISTS system_action_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL COMMENT 'Thời gian hành động',
    staff_id VARCHAR(20) COMMENT 'Mã nhân viên (có thể null nếu là hành động hệ thống)',
    action_type VARCHAR(50) NOT NULL COMMENT 'Loại hành động: LOGIN, VIEW_MEDICINE, CREATE_PRESCRIPTION, IMPORT_STOCK, ...',
    entity_type VARCHAR(50) COMMENT 'Loại đối tượng: MEDICINE, PRESCRIPTION, INVENTORY, STAFF...',
    entity_id VARCHAR(50) COMMENT 'ID đối tượng liên quan',
    success TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=Thành công, 0=Thất bại',
    client_ip VARCHAR(50) COMMENT 'Địa chỉ IP / thiết bị',
    user_agent VARCHAR(255) COMMENT 'Thông tin client (nếu có)',
    detail TEXT COMMENT 'Mô tả chi tiết / lý do lỗi',
    request_payload TEXT COMMENT 'Dữ liệu request (JSON, serialized...)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff_info(staff_id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_timestamp (timestamp),
    INDEX idx_staff_id (staff_id),
    INDEX idx_action_type (action_type),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Log hành vi hệ thống (audit trail)';

-- ============================================
-- 8.1. Bảng quản lý thẻ và khóa (Card Keys)
--   - Lưu thông tin khóa mã hóa cho mỗi thẻ
--   - Card ID format: [1B version][1B card type (role)][2B issue counter][12B random/sequence] = 16 bytes
-- ============================================
CREATE TABLE IF NOT EXISTS card_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id VARCHAR(64) UNIQUE NOT NULL COMMENT 'ID thẻ (16 bytes hex = 32 chars, do server tạo)',
    static_key_encrypted BLOB NOT NULL COMMENT 'Khóa tĩnh đã mã hóa (AES-256)',
    static_key_iv BLOB NOT NULL COMMENT 'IV cho static key encryption (16 bytes)',
    public_key_rsa BLOB NOT NULL COMMENT 'Khóa công khai RSA (2048-bit, X.509 format)',
    public_key_format VARCHAR(20) DEFAULT 'X.509' COMMENT 'Định dạng public key',
    staff_id VARCHAR(20) COMMENT 'FK -> staff_info.staff_id (nếu có)',
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian nạp thẻ',
    last_auth_at TIMESTAMP NULL COMMENT 'Lần xác thực gần nhất',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=Active, 0=Revoked',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff_info(staff_id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_card_id (card_id),
    INDEX idx_staff_id (staff_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quản lý khóa thẻ';

-- ============================================
-- 8.2. Bảng session keys (Card Sessions)
--   - Lưu thông tin session cho audit và quản lý
--   - Tùy chọn, có thể xóa sau khi session hết hạn
-- ============================================
CREATE TABLE IF NOT EXISTS card_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id VARCHAR(64) NOT NULL COMMENT 'FK -> card_keys.card_id',
    session_id VARCHAR(64) UNIQUE NOT NULL COMMENT 'Session ID duy nhất',
    challenge_server BLOB NOT NULL COMMENT 'Challenge từ server (32 bytes)',
    challenge_card BLOB NOT NULL COMMENT 'Challenge từ thẻ (32 bytes)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo session',
    expires_at TIMESTAMP NOT NULL COMMENT 'Thời gian hết hạn session',
    FOREIGN KEY (card_id) REFERENCES card_keys(card_id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_card_id (card_id),
    INDEX idx_session_id (session_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Session keys cho audit';

-- ============================================
-- 9. Views hữu ích
-- ============================================

-- View: Thuốc sắp hết hạn (trong 30 ngày) dựa trên lô thuốc
CREATE OR REPLACE VIEW vw_expiring_medicines AS
SELECT 
    b.medicine_code AS code,
    m.name,
    b.quantity,
    b.expiry_date,
    b.batch_number,
    DATEDIFF(b.expiry_date, CURDATE()) AS days_until_expiry
FROM medicine_batches b
JOIN medicines m ON m.code = b.medicine_code
WHERE b.expiry_date IS NOT NULL
  AND b.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
  AND b.expiry_date >= CURDATE()
  AND b.quantity > 0
ORDER BY b.expiry_date ASC;

-- View: Thuốc đã hết hạn dựa trên lô thuốc
CREATE OR REPLACE VIEW vw_expired_medicines AS
SELECT 
    b.medicine_code AS code,
    m.name,
    b.quantity,
    b.expiry_date,
    b.batch_number,
    DATEDIFF(CURDATE(), b.expiry_date) AS days_expired
FROM medicine_batches b
JOIN medicines m ON m.code = b.medicine_code
WHERE b.expiry_date IS NOT NULL
  AND b.expiry_date < CURDATE()
  AND b.quantity > 0
ORDER BY b.expiry_date DESC;

-- View: Thuốc sắp hết (tổng quantity theo medicines < min_quantity)
CREATE OR REPLACE VIEW vw_low_stock_medicines AS
SELECT 
    code,
    name,
    quantity,
    (CASE WHEN min_quantity > 0 THEN min_quantity ELSE 10 END) AS threshold
FROM medicines
WHERE quantity < (CASE WHEN min_quantity > 0 THEN min_quantity ELSE 10 END)
ORDER BY quantity ASC;

-- View: Thống kê xuất nhập theo ngày (từ inventory_logs)
CREATE OR REPLACE VIEW vw_daily_inventory_stats AS
SELECT 
    DATE(timestamp) AS date,
    type,
    COUNT(*) AS transaction_count,
    SUM(quantity_change) AS total_quantity_change,
    COUNT(DISTINCT medicine_code) AS unique_medicines,
    COUNT(DISTINCT staff_id) AS unique_staff
FROM inventory_logs
GROUP BY DATE(timestamp), type
ORDER BY date DESC, type;

-- View: Top nhân viên thao tác kho nhiều nhất
CREATE OR REPLACE VIEW vw_top_staff_activity AS
SELECT 
    s.staff_id,
    s.short_name,
    s.role,
    COUNT(l.id) AS total_transactions,
    SUM(CASE WHEN l.type = 1 THEN -l.quantity_change ELSE 0 END) AS total_export,
    SUM(CASE WHEN l.type = 0 THEN l.quantity_change ELSE 0 END) AS total_import
FROM staff_info s
LEFT JOIN inventory_logs l ON s.staff_id = l.staff_id
GROUP BY s.staff_id, s.short_name, s.role
ORDER BY total_transactions DESC;

-- ============================================
-- 10. Stored Procedures
-- ============================================

-- Procedure: Kiểm tra nhanh thông tin thuốc theo mã
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_quick_check_medicine(IN p_code VARCHAR(20))
BEGIN
    SELECT 
        m.code,
        m.name,
        m.quantity,
        m.min_quantity,
        (CASE WHEN m.min_quantity > 0 THEN m.min_quantity ELSE 10 END) AS threshold,
        (SELECT SUM(b.quantity) FROM medicine_batches b WHERE b.medicine_code = m.code) AS total_batch_quantity
    FROM medicines m
    WHERE m.code = p_code;
END //
DELIMITER ;

-- Procedure: Thống kê tổng quan kho
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_inventory_summary()
BEGIN
    SELECT 
        (SELECT COUNT(*) FROM medicines) AS total_medicines,
        (SELECT COALESCE(SUM(quantity), 0) FROM medicines) AS total_quantity,
        (SELECT COUNT(*) FROM vw_low_stock_medicines) AS low_stock_count,
        (SELECT COUNT(*) FROM vw_expired_medicines) AS expired_count,
        (SELECT COUNT(*) FROM vw_expiring_medicines) AS expiring_soon_count;
END //
DELIMITER ;

-- ============================================
-- 11. Sample Data (Optional - để test)
-- ============================================

-- Insert sample staff (bao gồm 1 tài khoản admin)
INSERT INTO staff_info (staff_id, short_name, full_name, role, department) VALUES
('ADMIN001', 'System Admin', 'Quản trị hệ thống', 0, 'Ban Quản Trị'),
('DS001', 'Tran Thi B', 'Dược sĩ Trần Thị B', 1, 'Khoa Dược'),
('KHO001', 'Le Van C', 'Nhân viên kho Lê Văn C', 2, 'Kho Thuốc')
ON DUPLICATE KEY UPDATE short_name=VALUES(short_name), full_name=VALUES(full_name), role=VALUES(role);

-- Tài khoản đăng nhập admin mẫu:
--  username: admin
--  password: admin123  (được hash bằng SHA2 trong MySQL)
INSERT INTO admin_accounts (staff_id, username, password_hash, active)
VALUES ('ADMIN001', 'admin', SHA2('admin123', 256), 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username);

-- Insert sample medicines (master)
INSERT INTO medicines (code, name, unit, quantity, min_quantity) VALUES
('MED001', 'Paracetamol 500mg', 'viên', 100, 10),
('MED002', 'Aspirin 81mg', 'viên', 50, 10),
('MED003', 'Amoxicillin 250mg', 'viên', 30, 10),
('MED004', 'Ibuprofen 400mg', 'viên', 20, 10),
('MED005', 'Naproxen 500mg', 'viên', 15, 10),
('MED006', 'Cetirizine 10mg', 'viên', 10, 10),
('MED007', 'Doxycycline 100mg', 'viên', 8, 10),
('MED008', 'Azithromycin 500mg', 'viên', 5, 10),
('MED009', 'Levocetirizine 5mg', 'viên', 3, 10),
('MED010', 'Ciprofloxacin 500mg', 'viên', 2, 10),
ON DUPLICATE KEY UPDATE name=VALUES(name), unit=VALUES(unit);

-- Insert sample batches
INSERT INTO medicine_batches (medicine_code, batch_number, expiry_date, import_date, quantity, location) VALUES
('MED001', 'LOT2024001', '2025-12-31', '2024-01-15', 60, 'Kệ A1'),
('MED001', 'LOT2024002', '2026-06-30', '2024-06-01', 40, 'Kệ A2'),
('MED002', 'LOT2024003', '2025-06-30', '2024-02-01', 50, 'Kệ B1'),
('MED003', 'LOT2023001', '2024-12-31', '2023-12-01', 30, 'Kệ C1')
ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);



