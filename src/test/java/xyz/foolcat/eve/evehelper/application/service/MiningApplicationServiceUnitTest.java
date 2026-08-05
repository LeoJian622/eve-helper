package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.domain.service.system.MiningDetailService;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 月矿采掘应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("月矿采掘应用服务单元测试")
class MiningApplicationServiceUnitTest {

    @Mock
    MiningDetailService miningDetailService;

    private MiningApplicationService miningApplicationService;

    @BeforeEach
    void setUp() {
        miningApplicationService = new MiningApplicationService(miningDetailService);
    }

    @Test
    @DisplayName("同步成功 -> 调用 saveObserverMining")
    void syncMiningByObserver_success() throws Exception {
        miningApplicationService.syncMiningByObserver(1, 2L);

        verify(miningDetailService).saveObserverMining(1, 2L);
    }

    @Test
    @DisplayName("同步抛 ParseException -> 转 EveHelperException")
    void syncMiningByObserver_parseError_throws() throws Exception {
        doThrow(new ParseException("bad", 0)).when(miningDetailService).saveObserverMining(1, 2L);

        assertThrows(EveHelperException.class,
                () -> miningApplicationService.syncMiningByObserver(1, 2L));
    }
}