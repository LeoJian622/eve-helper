package xyz.foolcat.eve.evehelper.application.dto.response;

import xyz.foolcat.eve.evehelper.domain.model.entity.system.IndustryJob;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 工业直查分页响应(022 US3)。
 *
 * <p>字段类型逐字取自 {@link IndustryJob} 实体 getter,
 * 不做类型转换或字段重命名。</p>
 *
 * @param jobs  当前页工业订单列表
 * @param total 过滤后总数(非分页后总数)
 * @author Leojan
 */
public record IndustryJobPageResponse(List<IndustryJobItem> jobs, int total) {

    /**
     * 单条工业订单 DTO。
     *
     * <p>字段与 {@link IndustryJob} 实体一一对应,类型逐字取自 getter。</p>
     */
    public record IndustryJobItem(
            Long jobId,
            Integer activityId,
            String status,
            Integer productTypeId,
            Integer runs,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long installerId,
            Long facilityId
    ) {

        /**
         * 从领域实体构造 DTO。
         *
         * @param job 领域实体
         * @return DTO
         */
        public static IndustryJobItem from(IndustryJob job) {
            return new IndustryJobItem(
                    job.getJobId(),
                    job.getActivityId(),
                    job.getStatus(),
                    job.getProductTypeId(),
                    job.getRuns(),
                    job.getStartDate(),
                    job.getEndDate(),
                    job.getInstallerId(),
                    job.getFacilityId()
            );
        }
    }
}
