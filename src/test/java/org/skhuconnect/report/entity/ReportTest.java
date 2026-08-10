package org.skhuconnect.report.entity;

import org.junit.jupiter.api.Test; import org.skhuconnect.admin.entity.Admin; import org.skhuconnect.petition.entity.Petition; import org.skhuconnect.user.entity.User; import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.mock;

class ReportTest {
 @Test void usesRequiredReasonTypes(){assertThat(ReportReasonType.values()).containsExactly(ReportReasonType.SPAM,ReportReasonType.ABUSE,ReportReasonType.INAPPROPRIATE,ReportReasonType.FALSE_INFORMATION,ReportReasonType.OTHER);}
 @Test void requiresTenCharacterDetailAndAllowsOnlyPendingProcessing(){Report r=Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.SPAM,"1234567890"); assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING); assertThatThrownBy(()->Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.OTHER,"short")).isInstanceOf(IllegalArgumentException.class); r.process(ReportStatus.DISMISSED,mock(Admin.class),"ok",LocalDateTime.now()); assertThatThrownBy(()->r.process(ReportStatus.ACTION_TAKEN,mock(Admin.class),"again",LocalDateTime.now())).isInstanceOf(IllegalStateException.class);}
}