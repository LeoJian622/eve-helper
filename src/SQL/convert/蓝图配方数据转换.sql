###把SDE的蓝图数据库转换系统蓝图数据库
insert into eve_helper.blueprints_data
select it.typeID,
       it.typeName,
       iap.blueprintTypeID,
       it1.typeName,
       iap.activityID,
       iap.quantity,
       iap.probability,
       iA.time,
       iB.maxProductionLimit
from industryActivityProducts iap
         left join invTypes it on iap.productTypeID = it.typeID
         left join invTypes it1 on blueprintTypeID = it1.typeID
         left join industryActivities iA on iap.blueprintTypeID = iA.blueprintTypeID and iap.activityID = iA.activityID
         left join industryBlueprints iB on iap.blueprintTypeID = iB.blueprintTypeID
where it.typeID is not null
order by typeID;



### 蓝图配方数据
insert into eve_helper.blueprint_formula select iAM.blueprintTypeID,iAM.materialTypeID,it.typeName,activityID,quantity from industryActivityMaterials iAM left join invTypes it on iAM.materialTypeID = it.typeID;

