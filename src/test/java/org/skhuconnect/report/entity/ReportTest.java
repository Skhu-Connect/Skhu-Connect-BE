package org.skhuconnect.report.entity;

import org.junit.jupiter.api.Test; import org.skhuconnect.admin.entity.Admin; import org.skhuconnect.petition.entity.Petition; import org.skhuconnect.user.entity.User; import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.mock;

class ReportTest {
 @Test void usesRequiredReasonTypes(){assertThat(ReportReasonType.values()).containsExactly(ReportReasonType.SPAM,ReportReasonType.ABUSE,ReportReasonType.INAPPROPRIATE,ReportReasonType.FALSE_INFORMATION,ReportReasonType.OTHER);}
 @Test void requiresTenCharacterDetailAndAllowsOnlyPendingProcessing(){Report r=Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.SPAM,"1234567890"); assertThat(r.getStatus()).isEqualTo(ReportStatus.PENDING); assertThatThrownBy(()->Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.OTHER,"short")).isInstanceOf(IllegalArgumentException.class); r.process(ReportStatus.DISMISSED,null,mock(Admin.class),"ok",LocalDateTime.now()); assertThatThrownBy(()->r.process(ReportStatus.ACTION_TAKEN,ReportActionType.HIDE,mock(Admin.class),"again",LocalDateTime.now())).isInstanceOf(IllegalStateException.class);}
 @Test void actionTakenRequiresActionTypeAndDismissedForbidsIt(){Report r1=Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.SPAM,"1234567890"); assertThatThrownBy(()->r1.process(ReportStatus.ACTION_TAKEN,null,mock(Admin.class),"ok",LocalDateTime.now())).isInstanceOf(IllegalStateException.class);
  Report r2=Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.SPAM,"1234567890"); assertThatThrownBy(()->r2.process(ReportStatus.DISMISSED,ReportActionType.HIDE,mock(Admin.class),"ok",LocalDateTime.now())).isInstanceOf(IllegalStateException.class);}
 @Test void storesActionTypeOnActionTaken(){Report r=Report.forPetition(mock(User.class),mock(Petition.class),ReportReasonType.SPAM,"1234567890"); r.process(ReportStatus.ACTION_TAKEN,ReportActionType.USER_LOGIN_BAN,mock(Admin.class),"ok",LocalDateTime.now()); assertThat(r.getActionType()).isEqualTo(ReportActionType.USER_LOGIN_BAN);}
}