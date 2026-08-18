package xyz.foolcat.eve.evehelper.infrastructure.assembler.esi;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.WalletTransaction;
import xyz.foolcat.eve.evehelper.infrastructure.external.esi.model.WalletTransactionsResponse;

import java.time.OffsetDateTime;

/**
 * ESI 钱包交易响应 → 领域实体转换器(防腐层)。
 * <p>
 * 仅映射 ESI 业务字段;归属字段(ownerType/ownerId/division)由服务层回填,此处忽略。
 * date 从 ESI 字符串按 ISO-8601(OffsetDateTime)解析。
 *
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface EsiWalletTransactionConverter {

    /**
     * WalletTransactionsResponse → WalletTransaction(不填归属字段)。
     */
    @Mappings({
            @Mapping(target = "ownerType", ignore = true),
            @Mapping(target = "ownerId", ignore = true),
            @Mapping(target = "division", ignore = true),
            @Mapping(target = "date", expression = "java(parseDate(walletTransactionsResponse.getDate()))")
    })
    WalletTransaction toDomain(WalletTransactionsResponse walletTransactionsResponse);

    /**
     * ESI date 字符串(ISO-8601 带偏移,如 2020-11-23T13:22:08Z)解析为 OffsetDateTime。
     */
    default OffsetDateTime parseDate(String date) {
        if (date == null) {
            return null;
        }
        return OffsetDateTime.parse(date);
    }
}