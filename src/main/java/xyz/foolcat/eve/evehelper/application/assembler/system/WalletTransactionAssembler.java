package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletTransactionVO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;

import java.util.List;

/**
 * 钱包交易组装器(领域实体 -> 响应 VO)。
 * <p>WalletTransaction 与 WalletTransactionVO 的 date 均为 {@link java.time.OffsetDateTime},
 * 同名同型,MapStruct 直接映射无歧义。ownerType/ownerId/division 等归属字段由领域实体回填后映射。</p>
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface WalletTransactionAssembler {

    WalletTransactionVO toVo(WalletTransaction source);

    List<WalletTransactionVO> toVo(List<WalletTransaction> sourceList);
}