package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EveAccountPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EveAccount 仓储删除方法集成测试（T040）。
 * <p>
 * 真实测试库 eve_helper_test（阿里云 RDS），eve_account 表已就绪。
 * 测试隔离：合成 userId 999022005/999022006，characterId 999022104/999022105，
 * characterName T040TEST-Char1/2，corpId 99902203。
 * 无 @Transactional（多数据源事务管理器歧义），@AfterEach 用 mapper 清理。
 *
 * @author Leojian
 * date 2026-11-13
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("T040 EveAccount 仓储删除方法集成测试")
class EveAccountRepositoryDeleteTest {

    private static final Integer TEST_USER_5 = 999022005;
    private static final Integer TEST_USER_6 = 999022006;
    private static final Integer CHAR_ID_4 = 999022104;
    private static final Integer CHAR_ID_5 = 999022105;
    private static final Integer CORP_ID = 99902203;

    @Autowired
    EveAccountRepository eveAccountRepository;

    @Autowired
    EveAccountMapper eveAccountMapper;

    @BeforeEach
    void setUp() {
        // 插入两行合成数据：user5×2
        insertTestRow(CHAR_ID_4, "T040TEST-Char1", CORP_ID, TEST_USER_5);
        insertTestRow(CHAR_ID_5, "T040TEST-Char2", CORP_ID, TEST_USER_5);
    }

    @AfterEach
    void cleanup() {
        eveAccountMapper.delete(
                new QueryWrapper<EveAccountPO>()
                        .in("character_id", CHAR_ID_4, CHAR_ID_5)
        );
    }

    private void insertTestRow(Integer characterId, String characterName,
                                Integer corpId, Integer userId) {
        EveAccountPO po = new EveAccountPO();
        po.setCharacterId(characterId.longValue());
        po.setCharacterName(characterName);
        po.setCorpId(corpId.longValue());
        po.setCorpName("T040TEST-Corp");
        po.setAllianceId(0L);
        po.setAllianceName("T040TEST-Alliance");
        po.setType(0);
        po.setQq(null);
        po.setUserId(userId.longValue());
        po.setRefreshToken(null);
        eveAccountMapper.insert(po);
    }

    // ─── 用例 1: delete_ownBinding_removesRow ──────────────────

    @Test
    @DisplayName("deleteByUserIdAndCharacterId 删除自己的绑定：返回 1 + 行已移除")
    void delete_ownBinding_removesRow() {
        int deleted = eveAccountRepository.deleteByUserIdAndCharacterId(TEST_USER_5, CHAR_ID_4);

        assertThat(deleted).isEqualTo(1);

        Long remaining = eveAccountMapper.selectCount(
                new QueryWrapper<EveAccountPO>().eq("character_id", CHAR_ID_4)
        );
        assertThat(remaining).isZero();
    }

    // ─── 用例 2: delete_otherUsersBinding_returnsZero ──────────

    @Test
    @DisplayName("deleteByUserIdAndCharacterId 删除他人的绑定：返回 0 + 行仍在")
    void delete_otherUsersBinding_returnsZero() {
        int deleted = eveAccountRepository.deleteByUserIdAndCharacterId(TEST_USER_6, CHAR_ID_4);

        assertThat(deleted).isZero();

        Long remaining = eveAccountMapper.selectCount(
                new QueryWrapper<EveAccountPO>().eq("character_id", CHAR_ID_4)
        );
        assertThat(remaining).isEqualTo(1L);
    }
}
