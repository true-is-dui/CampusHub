package com.campushub.service.impl;

import com.campushub.common.BusinessException;
import com.campushub.config.FileStorageProperties;
import com.campushub.entity.StoredFile;
import com.campushub.entity.enums.FileBusinessType;
import com.campushub.entity.enums.FileUsage;
import com.campushub.mapper.StoredFileMapper;
import com.campushub.util.ImageValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadImage_writesFileAndMetadata() throws Exception {
        StoredFileMapper mapper = mock(StoredFileMapper.class);
        when(mapper.insert(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile storedFile = invocation.getArgument(0);
            storedFile.setId(10L);
            return 1;
        });
        FileStorageServiceImpl service = newService(mapper);
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "me.png", "image/png", new byte[]{1, 2, 3});

        Long storedId = service.uploadImage(file, 7L, FileUsage.AVATAR, FileBusinessType.USER_AVATAR, 7L);

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(mapper).insert(captor.capture());
        StoredFile saved = captor.getValue();
        assertThat(storedId).isEqualTo(10L);
        assertThat(saved.getUploaderId()).isEqualTo(7L);
        assertThat(saved.getFileUsage()).isEqualTo(FileUsage.AVATAR);
        assertThat(saved.getOriginalFilename()).isEqualTo("me.png");
        assertThat(saved.getMimeType()).isEqualTo("image/png");
        assertThat(saved.getSha256()).hasSize(64);
        assertThat(saved.getStoragePath()).startsWith("avatar/");
        assertThat(Files.exists(tempDir.resolve(saved.getStoragePath()))).isTrue();
    }

    @Test
    void uploadImage_rejectsUnsupportedMimeAndExtension() {
        FileStorageServiceImpl service = newService(mock(StoredFileMapper.class));
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "me.gif", "image/gif", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.uploadImage(file, 7L, FileUsage.AVATAR, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void loadFile_returnsResource() {
        StoredFileMapper mapper = mock(StoredFileMapper.class);
        StoredFile storedFile = new StoredFile();
        storedFile.setId(1L);
        storedFile.setStoragePath("avatar/2026/06/a.png");
        storedFile.setMimeType("image/png");
        storedFile.setFileSize(3L);
        storedFile.setOriginalFilename("a.png");
        when(mapper.selectById(1L)).thenReturn(storedFile);
        FileStorageServiceImpl service = newService(mapper);
        Path file = tempDir.resolve(storedFile.getStoragePath());
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[]{1, 2, 3});
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }

        assertThat(service.loadFile(1L).getResource().exists()).isTrue();
        assertThat(service.loadFile(1L).getMimeType()).isEqualTo("image/png");
    }

    private FileStorageServiceImpl newService(StoredFileMapper mapper) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setRoot(tempDir.toString());
        return new FileStorageServiceImpl(mapper, properties, new ImageValidator());
    }
}
