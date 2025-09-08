package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    private Long userId;
    private boolean hasImage;
    private String filename;
    private long size;
    private String contentType;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}
