package com.huydung.custmapapplication.music;

import com.huydung.custmapapplication.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một bài / playlist nghe trực tuyến được nhúng vào thư viện chung (Spotify, YouTube, SoundCloud).
 * Chỉ lưu dịch vụ + loại + mã đã kiểm tra (xem {@link MusicEmbeds#parse}); đường link khung phát được server dựng lại
 * từ các phần này, không bao giờ lấy nguyên link người dùng dán để đưa vào iframe.
 */
@Entity
@Table(name = "music_embeds", uniqueConstraints = @UniqueConstraint(name = "uk_music_embed_ref", columnNames = {"provider", "ref"}))
@Getter
@Setter
@NoArgsConstructor
public class MusicEmbedEntity extends BaseEntity {

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;        // MusicEmbeds.Provider

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;            // track | playlist | album | video ...

    @Column(name = "ref", nullable = false, length = 300)
    private String ref;             // mã bài / playlist (SoundCloud: đường dẫn "nguoi-dang/bai-hat")

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
