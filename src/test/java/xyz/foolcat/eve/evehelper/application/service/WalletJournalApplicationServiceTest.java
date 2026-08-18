package xyz.foolcat.eve.evehelper.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.foolcat.eve.evehelper.application.assembler.system.WalletJournalAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletJournalVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;
import xyz.foolcat.eve.evehelper.domain.port.cache.CacheGateway;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletJournalRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletJournalService;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * WalletJournalApplicationService 单元测试(TDD)。
 *
 * <p>纯 Mockito 单测(不起 Spring 上下文),聚焦军团钱包流水用例的契约:</p>
 * <ul>
 *     <li>军团同步:归属校验先于业务逻辑,ParseException 收敛为 EveHelperException,冷却校验</li>
 *     <li>军团分页:division 合法性校验(0/8 拒绝,null 拒绝),归属校验,仓储查询</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletJournalApplicationService 军团钱包流水用例")
class WalletJournalApplicationServiceTest {

    private static final int CORP_ID = 5001;

    @Mock
    private AccessGuard accessGuard;

    @Mock
    private WalletJournalService walletJournalService;

    @Mock
    private WalletJournalRepository walletJournalRepository;

    @Mock
    private WalletJournalAssembler walletJournalAssembler;

    @Mock
    private CacheGateway cacheGateway;

    @InjectMocks
    private WalletJournalApplicationService applicationService;

    /* ============================ 军团同步 ============================ */

    @Nested
    @DisplayName("syncCorporationJournal 军团钱包流水同步")
    class CorpSync {

        @Test
        @DisplayName("归属校验先于业务逻辑,随后委托领域服务")
        void guardsOwnershipThenDelegates() throws Exception {
            applicationService.syncCorporationJournal(CORP_ID);

            verify(accessGuard).requireOwnership(String.valueOf(CORP_ID), "军团钱包流水同步");
            verify(walletJournalService).syncCorporationJournal(CORP_ID);
        }

        @Test
        @DisplayName("领域层 ParseException 收敛为 EveHelperException")
        void parseException_wrappedAsEveHelperException() throws Exception {
            doThrow(new java.text.ParseException("bad jwt", 0))
                    .when(walletJournalService).syncCorporationJournal(any());

            assertThatThrownBy(() -> applicationService.syncCorporationJournal(CORP_ID))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("军团钱包流水同步失败");
        }

        @Test
        @DisplayName("冷却期内重复同步被拒绝,不触达领域服务")
        void cooldownActive_rejectedWithoutDelegation() {
            ReflectionTestUtils.setField(applicationService, "syncCooldownSeconds", 60L);
            when(cacheGateway.hasKey("wallet:journal:sync:corp:" + CORP_ID)).thenReturn(Boolean.TRUE);

            assertThatThrownBy(() -> applicationService.syncCorporationJournal(CORP_ID))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("同步操作过于频繁");
            verifyNoInteractions(walletJournalService);
        }
    }

    /* ============================ 军团分页 ============================ */

    @Nested
    @DisplayName("queryCorporationPage 军团钱包流水分页")
    class CorpPage {

        @ParameterizedTest
        @ValueSource(ints = {0, 8, -1})
        @DisplayName("division 越界(0/8/-1)抛 EveHelperException,不触达归属校验")
        void invalidDivision_rejected(int division) {
            assertThatThrownBy(() -> applicationService.queryCorporationPage(CORP_ID, division, 1, 20))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("军团分账参数不合法");
            verifyNoInteractions(accessGuard);
        }

        @Test
        @DisplayName("division 为空抛 EveHelperException,不触达归属校验")
        void nullDivision_rejected() {
            assertThatThrownBy(() -> applicationService.queryCorporationPage(CORP_ID, null, 1, 20))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("军团分账参数不合法");
            verifyNoInteractions(accessGuard);
        }

        @Test
        @DisplayName("corpId 为空抛 EveHelperException,不触达归属校验")
        void nullCorpId_rejected() {
            assertThatThrownBy(() -> applicationService.queryCorporationPage(null, 1, 1, 20))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("军团分账参数不合法");
            verifyNoInteractions(accessGuard);
        }

        @Test
        @DisplayName("入参合法:归属校验 → 以 (page, corpId, division) 查仓储 → 装配为 VO 分页")
        void valid_guardsAndQueriesMapsToVo() {
            WalletJournal domain = new WalletJournal();
            WalletJournalVO vo = new WalletJournalVO();
            IPage<WalletJournal> repoPage = new Page<>(1, 20, 1);
            repoPage.setRecords(List.of(domain));
            when(walletJournalRepository.selectPageByOwnerAndDivision(any(), eq((long) CORP_ID), eq(2)))
                    .thenReturn(repoPage);
            when(walletJournalAssembler.toVo(anyList())).thenReturn(List.of(vo));

            PageResult<WalletJournalVO> result = applicationService.queryCorporationPage(CORP_ID, 2, 1, 20);

            verify(accessGuard).requireOwnership(String.valueOf(CORP_ID), "军团钱包流水");
            verify(walletJournalRepository)
                    .selectPageByOwnerAndDivision(any(), eq((long) CORP_ID), eq(2));
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0)).isSameAs(vo);
            assertThat(result.getTotal()).isEqualTo(1L);
        }
    }
}
