package xyz.foolcat.eve.evehelper.infrastructure.assembler.persistence;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.WalletTransactionPO;

import java.util.List;

/**
 * WalletTransaction 领域实体与 WalletTransactionPO 持久化对象转换器(基础设施层)。
 *
 * <p>类型对齐(plan D5):ownerId 在 domain/PO 均为 {@code Long}(库列 owner_id BIGINT);
 * date 为 {@code OffsetDateTime}。跨层 Integer(ESI 参数)→ Long(入库列)由 service 层
 * longValue() 显式回填,不在此转换器做隐式转换。</p>
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface WalletTransactionPoConverter {

    /**
     * WalletTransactionPO 转换 WalletTransaction
     * @param walletTransactionPO 持久化对象
     * @return 领域实体
     */
    WalletTransaction po2Domain(WalletTransactionPO walletTransactionPO);

    /**
     * WalletTransaction 转换 WalletTransactionPO
     * @param walletTransaction 领域实体
     * @return 持久化对象
     */
    WalletTransactionPO domain2Po(WalletTransaction walletTransaction);

    List<WalletTransaction> po2Domain(List<WalletTransactionPO> walletTransactionPOList);

    List<WalletTransactionPO> domain2Po(List<WalletTransaction> walletTransactionList);
}