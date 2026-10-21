package com.huydung.custmapapplication.setting.dto;

/**
 * Các mục thông tin được in lên ảnh khi chụp (true = in, false = không in).
 * Khi cập nhật, trường nào để null thì giữ nguyên giá trị hiện tại.
 */
public class CameraSettingsDTO {

    private Boolean showTime;          // Ngày giờ chụp
    private Boolean showCoordinates;   // Tọa độ GPS
    private Boolean showAddress;       // Địa chỉ
    private Boolean showCustomerName;  // Tên khách hàng
    private Boolean showAccount;       // Mã thuê bao (account)
    private Boolean showCabinetCode;   // Mã tủ
    private Boolean showStaff;         // Tên nhân viên chụp

    public Boolean getShowTime() { return showTime; }
    public void setShowTime(Boolean showTime) { this.showTime = showTime; }
    public Boolean getShowCoordinates() { return showCoordinates; }
    public void setShowCoordinates(Boolean showCoordinates) { this.showCoordinates = showCoordinates; }
    public Boolean getShowAddress() { return showAddress; }
    public void setShowAddress(Boolean showAddress) { this.showAddress = showAddress; }
    public Boolean getShowCustomerName() { return showCustomerName; }
    public void setShowCustomerName(Boolean showCustomerName) { this.showCustomerName = showCustomerName; }
    public Boolean getShowAccount() { return showAccount; }
    public void setShowAccount(Boolean showAccount) { this.showAccount = showAccount; }
    public Boolean getShowCabinetCode() { return showCabinetCode; }
    public void setShowCabinetCode(Boolean showCabinetCode) { this.showCabinetCode = showCabinetCode; }
    public Boolean getShowStaff() { return showStaff; }
    public void setShowStaff(Boolean showStaff) { this.showStaff = showStaff; }
}
