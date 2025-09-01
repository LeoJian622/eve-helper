package xyz.foolcat.eve.evehelper.application.assembler.system;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import xyz.foolcat.eve.evehelper.domain.model.entity.system.BlueprintFormula;
import xyz.foolcat.eve.evehelper.infrastructure.persistence.entity.system.BlueprintFormulaPO;

import java.util.List;

/**
 * 蓝图配方转换器
 * @author Leojan
 */
@Mapper(componentModel = "spring")
public interface BlueprintFormulaAssembler {

    /**
     * BlueprintFormulaPO 转换为 BlueprintFormula
     * @param bluePrintFormulaPO
     * @return
     */
    BlueprintFormula po2Domain(BlueprintFormulaPO bluePrintFormulaPO);

    /**
     * BlueprintFormula 转换为 BlueprintFormulaPO
     * @param bluePrintFormula
     * @return
     */
    @Mappings({
            @Mapping(target = "gmtCreate", ignore = true),
            @Mapping(target = "gmtModified", ignore = true)
    })
    BlueprintFormulaPO domain2Po(BlueprintFormula bluePrintFormula);

    List<BlueprintFormula> po2Domain(List<BlueprintFormulaPO> bluePrintFormulaPOs);

    List<BlueprintFormulaPO> domain2Po(List<BlueprintFormula> bluePrintFormulas);

} 