package xyz.foolcat.eve.evehelper.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.foolcat.eve.evehelper.application.assembler.system.AssetsAssembler;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资产应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("资产应用服务单元测试")
class AssetsApplicationServiceUnitTest {

    @Mock
    AssetsService assetsService;

    @Mock
    AssetsAssembler assetsAssembler;

    private AssetsApplicationService assetsApplicationService;

    @BeforeEach
    void setUp() {
        assetsApplicationService = new AssetsApplicationService(assetsService, assetsAssembler);
    }

    @Test
    @DisplayName("同步成功 -> 调用 saveAndUpdateAsserts")
    void syncAssets_success() throws Exception {
        assetsApplicationService.syncAssets(1);

        verify(assetsService).saveAndUpdateAsserts(1);
    }

    @Test
    @DisplayName("同步抛 ParseException -> 转 EveHelperException")
    void syncAssets_parseError_throws() throws Exception {
        doThrow(new ParseException("bad", 0)).when(assetsService).saveAndUpdateAsserts(1);

        assertThrows(EveHelperException.class, () -> assetsApplicationService.syncAssets(1));
    }

    @Test
    @DisplayName("查询清单 -> 返回转 VO 后的分页结果")
    void queryAssetsList_returnsVoPage() {
        Assets asset = new Assets();
        when(assetsService.getAssertsListById("1", 0, 30)).thenReturn(List.of(asset));
        AssetsVO vo = new AssetsVO();
        when(assetsAssembler.domain2Vo(List.of(asset))).thenReturn(List.of(vo));

        PageResult<AssetsVO> result = assetsApplicationService.queryAssetsList("1", 0, 30);

        assertTrue(result.getRecords().stream().anyMatch(v -> v == vo));
    }
}