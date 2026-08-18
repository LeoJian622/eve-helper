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
import xyz.foolcat.eve.evehelper.application.assembler.system.WalletTransactionAssembler;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletTransactionVO;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.domain.repository.system.WalletTransactionRepository;
import xyz.foolcat.eve.evehelper.domain.service.system.WalletTransactionService;
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
 * WalletTransactionApplicationService 单元测试(TDD)。
 *
 * <p>纯 Mockito 单测(不起 Spring 上下文),mock {@link AccessGuard}/{@link WalletTransactionService}/
 * {@link WalletTransactionRepository}/{@link WalletTransactionAssembler},
 * 聚焦人物钱包交易两个用例的接口层契约:</p>
 * <ul>
 *     <li>指针同步:归属校验先于业务逻辑(防 IDOR),ParseException 收敛为 EveHelperException</li>
 *     <li>分页查询:入参边界校验先于归属鉴定,归属校验先于业务逻辑;以
 *         (page, "character", cid.longValue(), 0) 调仓储并经装配器映射为 VO 分页</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletTransactionApplicationService 人物钱包交易用例")
class WalletTransactionApplicationServiceTest {

    private static final int CID = 9001;

    @Mock
    private AccessGuard accessGuard;

    @Mock
    private WalletTransactionService walletTransactionService;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletTransactionAssembler walletTransactionAssembler;

    @InjectMocks
    private WalletTransactionApplicationService applicationService;

    /* ============================ 人物同步 ============================ */

    @Nested
    @DisplayName("syncCharacterTransactions 人物钱包交易同步")
    class CharacterSync {

        @Test
        @DisplayName("归属校验先于业务逻辑,随后委托领域服务")
        void guardsOwnershipThenDelegates() throws Exception {
            applicationService.syncCharacterTransactions(CID);

            verify(accessGuard).requireOwnership("9001", "钱包交易同步");
            verify(walletTransactionService).syncCharacterTransactions(CID);
        }

        @Test
        @DisplayName("领域层 ParseException 收敛为 EveHelperException")
        void parseException_wrappedAsEveHelperException() throws Exception {
            doThrow(new java.text.ParseException("bad jwt", 0))
                    .when(walletTransactionService).syncCharacterTransactions(any());

            assertThatThrownBy(() -> applicationService.syncCharacterTransactions(CID))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("钱包交易同步失败");
        }
    }

    /* ============================ 人物分页 ============================ */

    @Nested
    @DisplayName("queryCharacterPage 人物钱包交易分页")
    class CharacterPage {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("入参不合法(current<1)抛 EveHelperException,不触达归属校验")
        void invalidCurrent_rejected(int current) {
            assertThatThrownBy(() -> applicationService.queryCharacterPage(CID, current, 20))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("分页参数不合法");
            verifyNoInteractions(accessGuard);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -5, 1001})
        @DisplayName("入参不合法(size<1 或 >1000)抛 EveHelperException,不触达归属校验")
        void invalidSize_rejected(int size) {
            assertThatThrownBy(() -> applicationService.queryCharacterPage(CID, 1, size))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("分页参数不合法");
            verifyNoInteractions(accessGuard);
        }

        @Test
        @DisplayName("cid 为空抛 EveHelperException,不触达归属校验")
        void nullCid_rejected() {
            assertThatThrownBy(() -> applicationService.queryCharacterPage(null, 1, 20))
                    .isInstanceOf(EveHelperException.class)
                    .hasMessageContaining("分页参数不合法");
            verifyNoInteractions(accessGuard);
        }

        @Test
        @DisplayName("入参合法:归属校验 → 以 (page, character, cid, 0) 查仓储 → 装配为 VO 分页")
        void valid_guardsAndQueriesOwnerMapsToVo() {
            WalletTransaction domain = new WalletTransaction();
            WalletTransactionVO vo = new WalletTransactionVO();
            IPage<WalletTransaction> repoPage = new Page<>(1, 20, 1);
            repoPage.setRecords(List.of(domain));
            when(walletTransactionRepository.selectPageByOwner(any(), eq("character"), eq((long) CID), eq(0)))
                    .thenReturn(repoPage);
            when(walletTransactionAssembler.toVo(anyList())).thenReturn(List.of(vo));

            PageResult<WalletTransactionVO> result = applicationService.queryCharacterPage(CID, 1, 20);

            // 归属校验先于业务逻辑(防 IDOR)
            verify(accessGuard).requireOwnership("9001", "钱包交易");
            // 仓储调用契约:ownerType=character / ownerId=cid / division=0
            verify(walletTransactionRepository)
                    .selectPageByOwner(any(), eq("character"), eq((long) CID), eq(0));
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0)).isSameAs(vo);
            assertThat(result.getTotal()).isEqualTo(1L);
        }
    }
}