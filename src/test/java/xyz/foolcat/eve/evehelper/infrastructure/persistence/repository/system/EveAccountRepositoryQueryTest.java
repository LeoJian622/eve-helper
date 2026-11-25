package xyz.foolcat.eve.evehelper.infrastructure.persistence.repository.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.EveAccount;
import xyz.foolcat.eve.evehelper.domain.repository.system.EveAccountRepository;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.EveAccountPO;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.mapper.system.EveAccountMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EveAccount 仓储查询扩展集成测试（T028）。
 * <p>
 * 真实测试库 eve_helper_test（阿里云 RDS），eve_account 表已就绪。
 * 测试隔离：合成 userId 999022003/999022004，characterId 999022101/102/103，
 * characterName T028TEST-Char1/2/3，corpId 99902201/02202。
 * 无 @Transactional（多数据源事务管理器歧义），@AfterEach 用 mapper 清理。
 *
 * @author Leojian
 * date 2026-11-13
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("T028 EveAccount 仓储查询扩展集成测试")
class EveAccountRepositoryQueryTest {

    private static final Integer TEST_USER_3 = 999022003;
    private static final Integer TEST_USER_4 = 999022004;
    private static final Integer CHAR_ID_1 = 999022101;
    private static final Integer CHAR_ID_2 = 999022102;
    private static final Integer CHAR_ID_3 = 999022103;
    private static final Integer CORP_ID_1 = 99902201;
    private static final Integer CORP_ID_2 = 99902202;

    @Autowired
    EveAccountRepository eveAccountRepository;

    @Autowired
    EveAccountMapper eveAccountMapper;

    @BeforeEach
    void setUp() {
        // 插入三行合成数据：corp1×2 + corp2×1
        insertTestRow(CHAR_ID_1, "T028TEST-Char1", CORP_ID_1, TEST_USER_3);
        insertTestRow(CHAR_ID_2, "T028TEST-Char2", CORP_ID_1, TEST_USER_3);
        insertTestRow(CHAR_ID_3, "T028TEST-Char3", CORP_ID_2, TEST_USER_4);
    }

    @AfterEach
    void cleanup() {
        eveAccountMapper.delete(
                new QueryWrapper<EveAccountPO>()
                        .in("character_id", CHAR_ID_1, CHAR_ID_2, CHAR_ID_3)
        );
    }

    private void insertTestRow(Integer characterId, String characterName,
                                Integer corpId, Integer userId) {
        EveAccountPO po = new EveAccountPO();
        po.setCharacterId(characterId.longValue());
        po.setCharacterName(characterName);
        po.setCorpId(corpId.longValue());
        po.setCorpName("T028TEST-Corp");
        po.setAllianceId(0L);
        po.setAllianceName("T028TEST-Alliance");
        po.setType(0);
        po.setQq(null);
        po.setUserId(userId.longValue());
        po.setRefreshToken(null);
        eveAccountMapper.insert(po);
    }

    // ─── 用例 1: listAll_returnsAllBindings ──────────────────

    @Test
    @DisplayName("listAll 返回全部绑定行：包含三合成 characterId + 抽查字段")
    void listAll_returnsAllBindings() {
        List<EveAccount> all = eveAccountRepository.listAll();

        // 包含性断言（共享表不精确 size）
        Set<Integer> charIds = all.stream()
                .map(EveAccount::getCharacterId)
                .collect(Collectors.toSet());
        assertThat(charIds).contains(CHAR_ID_1, CHAR_ID_2, CHAR_ID_3);

        // 抽查第一合成行字段
        EveAccount char1 = all.stream()
                .filter(a -> CHAR_ID_1.equals(a.getCharacterId()))
                .findFirst()
                .orElseThrow();
        assertThat(char1.getCorpId()).isEqualTo(CORP_ID_1);
        assertThat(char1.getUserId()).isEqualTo(TEST_USER_3);
        assertThat(char1.getCharacterName()).isEqualTo("T028TEST-Char1");
    }

    // ─── 用例 2: listByCorpId_filtersByCorp ─────────────────

    @Test
    @DisplayName("listByCorpId 按军团过滤：corp1 恰含 2 合成行且 corpId 一致")
    void listByCorpId_filtersByCorp() {
        List<EveAccount> corp1Accounts = eveAccountRepository.listByCorpId(CORP_ID_1);

        assertThat(corp1Accounts).hasSize(2);
        Set<Integer> charIds = corp1Accounts.stream()
                .map(EveAccount::getCharacterId)
                .collect(Collectors.toSet());
        assertThat(charIds).containsExactlyInAnyOrder(CHAR_ID_1, CHAR_ID_2);

        for (EveAccount account : corp1Accounts) {
            assertThat(account.getCorpId()).isEqualTo(CORP_ID_1);
        }

        // 不含 corp2 的 characterId
        assertThat(charIds).doesNotContain(CHAR_ID_3);
    }

    // ─── 用例 3: listByCorpId_unknownCorp_emptyList ─────────

    @Test
    @DisplayName("listByCorpId 未知军团返回空列表")
    void listByCorpId_unknownCorp_emptyList() {
        List<EveAccount> result = eveAccountRepository.listByCorpId(99902299);
        assertThat(result).isEmpty();
    }
}
