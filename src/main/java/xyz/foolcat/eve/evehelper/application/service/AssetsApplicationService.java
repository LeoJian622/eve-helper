package xyz.foolcat.eve.evehelper.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.foolcat.eve.evehelper.application.assembler.system.AssetsAssembler;
import xyz.foolcat.eve.evehelper.application.security.AccessGuard;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.Assets;
import xyz.foolcat.eve.evehelper.domain.service.system.AssetsService;
import xyz.foolcat.eve.evehelper.domain.model.vo.AssetsVO;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;
import xyz.foolcat.eve.evehelper.shared.kernel.exception.EveHelperException;
import xyz.foolcat.eve.evehelper.shared.util.PageResultUtil;

import java.text.ParseException;
import java.util.List;

/**
 * 资产应用服务
 * 负责资产同步与清单查询用例
 *
 * @author Leojan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetsApplicationService {

    private final AssetsService assetsService;
    private final AssetsAssembler assetsAssembler;
    private final AccessGuard accessGuard;

    /**
     * 从 ESI 同步资产数据。
     *
     * @param cid 人物或军团 ID
     */
    public void syncAssets(Integer cid) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物/军团属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(String.valueOf(cid), "资产同步");
        try {
            assetsService.saveAndUpdateAsserts(cid);
        } catch (ParseException e) {
            log.error("资产同步失败: cid={}", cid, e);
            throw new EveHelperException("资产同步失败", e);
        }
    }

    /**
     * 分页查询资产清单。
     *
     * @param cid     人物或军团 ID
     * @param current 页码
     * @param size    每页行数
     * @return 资产视图分页结果
     */
    public PageResult<AssetsVO> queryAssetsList(String cid, int current, int size) {
        // 归属校验先于业务逻辑:cid 为用户可控入参,须先确认该人物/军团属于当前用户(防御 IDOR)
        accessGuard.requireOwnership(cid, "资产清单");
        IPage<Assets> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        List<Assets> records = assetsService.getAssertsListById(cid, current, size);
        page.setRecords(records);
        return PageResultUtil.copy(page, assetsAssembler::domain2Vo);
    }
}