package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import xyz.foolcat.eve.evehelper.application.dto.response.WalletJournalVO;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletJournal;

import java.util.List;

/**
 * 钱包流水组装器(领域实体 -> 响应 VO)。
 * <p>WalletJournal 与 WalletJournalVO 的 date 均为 {@link java.time.OffsetDateTime},同名同型,MapStruct 直接映射无歧义。</p>
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface WalletJournalAssembler {

    WalletJournalVO toVo(WalletJournal source);

    List<WalletJournalVO> toVo(List<WalletJournal> sourceList);
}