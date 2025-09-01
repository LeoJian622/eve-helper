package xyz.foolcat.eve.evehelper.shared.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import xyz.foolcat.eve.evehelper.shared.kernel.base.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * @author yongj
 * date 2025-07-29 17:10
 */

public class PageResultUtil {

    public static <T,R> PageResult<R> copy(PageResult<T> source, Function<List<T>,List<R>> converter) {
        return PageResult.<R>builder()
                .records(converter.apply(source.getRecords()))
                .total(source.getTotal())
                .current(source.getCurrent())
                .size(source.getSize())
                .pages(source.getPages())
                .hasNext(source.getHasNext())
                .hasPrevious(source.getHasPrevious())
                .build();
    }

    public static <T,R> PageResult<R> copy(IPage<T> source, Function<List<T>,List<R>> converter) {
        return PageResult.<R>builder()
                .records(converter.apply(source.getRecords()))
                .total(source.getTotal())
                .current(source.getCurrent())
                .size(source.getSize())
                .pages(source.getPages())
                .hasNext(source.getCurrent() + 1 < source.getPages())
                .hasPrevious(source.getCurrent() > 1)
                .build();
    }

}
