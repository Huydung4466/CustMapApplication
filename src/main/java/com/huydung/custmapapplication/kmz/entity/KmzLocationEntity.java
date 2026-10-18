package com.huydung.custmapapplication.kmz.entity;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kmz_locations", indexes = {
        @Index(name = "idx_kmz_cabinet_code", columnList = "cabinet_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KmzLocationEntity extends BaseEntity {
    // Mã tủ (VD: DLK0412-FD01-DN01-SN04 hoặc DLK0010-DN01-SN01)
    @Column(name = "cabinet_code", unique = true, nullable = false, length = 100)
    private String cabinetCode;

    // Vĩ độ (VD: 12.943032)
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    // Kinh độ (VD: 108.25637)
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    // Tên file KMZ nguồn (VD: Buon Ho_T9.kmz)
    @Column(name = "source_file", length = 255)
    private String sourceFile;
}
