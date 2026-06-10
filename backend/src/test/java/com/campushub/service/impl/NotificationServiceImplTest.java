package com.campushub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BusinessException;
import com.campushub.common.ErrorCode;
import com.campushub.common.PageQuery;
import com.campushub.common.PageResult;
import com.campushub.dto.notification.NotificationItem;
import com.campushub.dto.notification.ReadStatus;
import com.campushub.entity.NotificationRecord;
import com.campushub.entity.enums.NotificationType;
import com.campushub.mapper.NotificationRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationServiceImpl} 单元测试：mock mapper，验证创建、分页查询的 VO 映射、
 * 未读计数与标记已读的鉴权/幂等分支，不连数据库。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRecordMapper notificationRecordMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createNotice_buildsRecordAsUnread() {
        notificationService.createNotice(7L, NotificationType.PICKUP, "标题", "内容",
                "PICKUP_REQUEST", 10L);

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordMapper).insert(captor.capture());
        NotificationRecord saved = captor.getValue();
        assertThat(saved.getReceiverId()).isEqualTo(7L);
        assertThat(saved.getType()).isEqualTo(NotificationType.PICKUP);
        assertThat(saved.getBusinessType()).isEqualTo("PICKUP_REQUEST");
        assertThat(saved.getBusinessId()).isEqualTo(10L);
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    void createNotice_swallowsInsertFailure_doesNotThrow() {
        doThrow(new RuntimeException("db down")).when(notificationRecordMapper).insert(any(NotificationRecord.class));

        // 通知是旁路功能：insert 失败不应向调用方（主业务）抛出。
        assertThatCode(() -> notificationService.createNotice(7L, NotificationType.SYSTEM,
                "标题", "内容", null, null)).doesNotThrowAnyException();
    }

    @Test
    void queryMyNotices_mapsReadStatusToVo() {
        NotificationRecord unread = record(1L, false);
        NotificationRecord read = record(2L, true);
        Page<NotificationRecord> page = new Page<>(1, 20);
        page.setRecords(List.of(unread, read));
        page.setTotal(2);
        when(notificationRecordMapper.selectPage(any(IPage.class), any())).thenReturn(page);

        PageResult<NotificationItem> result = notificationService.queryMyNotices(7L, new PageQuery());

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).hasSize(2);
        assertThat(result.getList().get(0).getReadStatus()).isEqualTo(ReadStatus.UNREAD);
        assertThat(result.getList().get(1).getReadStatus()).isEqualTo(ReadStatus.READ);
    }

    @Test
    void countUnread_returnsMapperCount() {
        when(notificationRecordMapper.selectCount(any())).thenReturn(3L);

        assertThat(notificationService.countUnread(7L)).isEqualTo(3L);
    }

    @Test
    void markRead_success_updatesRecord() {
        NotificationRecord record = record(1L, false);
        when(notificationRecordMapper.selectById(1L)).thenReturn(record);

        notificationService.markRead(1L, 7L);

        assertThat(record.getIsRead()).isTrue();
        assertThat(record.getReadAt()).isNotNull();
        verify(notificationRecordMapper).updateById(record);
    }

    @Test
    void markRead_notFound_throws404() {
        when(notificationRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> notificationService.markRead(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void markRead_notReceiver_throws403() {
        when(notificationRecordMapper.selectById(1L)).thenReturn(record(1L, false));

        // 通知接收者是 7L，另一个用户 8L 无权操作。
        assertThatThrownBy(() -> notificationService.markRead(1L, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verify(notificationRecordMapper, never()).updateById(any(NotificationRecord.class));
    }

    @Test
    void markRead_alreadyRead_isIdempotent() {
        when(notificationRecordMapper.selectById(1L)).thenReturn(record(1L, true));

        notificationService.markRead(1L, 7L);

        verify(notificationRecordMapper, never()).updateById(any(NotificationRecord.class));
    }

    private NotificationRecord record(Long id, boolean read) {
        NotificationRecord r = new NotificationRecord();
        r.setId(id);
        r.setReceiverId(7L);
        r.setType(NotificationType.PICKUP);
        r.setTitle("标题");
        r.setContent("内容");
        r.setBusinessType("PICKUP_REQUEST");
        r.setBusinessId(10L);
        r.setIsRead(read);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }
}
