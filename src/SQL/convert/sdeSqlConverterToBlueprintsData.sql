create table if not exists assets
(
    item_id           bigint      not null comment '物品ID'
        primary key,
    type_id           int         null comment 'invTypeID',
    location_id       bigint      null comment '建筑ID',
    location_type     varchar(36) null comment '类型station, solar_system, item, other',
    location_flag     varchar(64) null,
    is_singleton      tinyint(1)  null,
    is_blueprint_copy tinyint(1)  null comment '蓝图拷贝',
    quantity          bigint      null comment '数量',
    owner_id          bigint      null comment '所有者ID'
);

create table if not exists blueprint_formula
(
    blueprint_id     int              null comment '蓝图ID',
    material_type_id int              null comment '原材料typeid',
    type_name        varchar(500)     null comment '名称',
    activity_id      tinyint unsigned null comment '活动类型',
    quantity         int              null comment '需求数量'
)
    comment '蓝图材料配方基础数据';

create table if not exists blueprints
(
    item_id             bigint        not null comment '唯一item id'
        primary key,
    type_id             int           null comment 'invType id',
    material_efficiency int default 0 null comment '材料效率',
    time_efficiency     int default 0 null comment '时间效率',
    runs                int           null comment '流程数',
    quantity            bigint        null comment '数量',
    location_id         bigint        null comment '建筑ID',
    location_flag       varchar(64)   null comment '位置',
    owner_id            bigint        null comment '所有者ID'
)
    comment '蓝图属性';

create table if not exists blueprints_data
(
    type_id              int          not null comment '产物typeId',
    type_name            varchar(500) null comment '产物名称',
    blueprint_type_id    int          null comment '蓝图typeId',
    blueprint_name       varchar(500) null comment '蓝图名称',
    activity_id          tinyint      null comment '生产类型',
    quantity             int          null comment '单流程产量',
    probability          float        null comment '基础成功率',
    time                 int          null comment '生产基础时间',
    max_production_limit int          null comment '最大单次产量'
)
    comment '蓝图生产相关数据';

create index blueprints_data_pk_blueprint_type_id
    on blueprints_data (blueprint_type_id);

create index blueprints_data_pk_type_id
    on blueprints_data (type_id);

create table if not exists esi_config
(
    id           int auto_increment
        primary key,
    base_url     varchar(128) not null comment 'ESI请求地址',
    version      varchar(32)  not null comment 'ESI版本',
    type         int          not null comment 'CH:0 EU:1',
    gmt_create   datetime     null,
    gmt_modified datetime     null
)
    comment 'ESI配置表';

create table if not exists eve_account
(
    type           int         null comment 'CH:0 EU:1',
    gmt_modified   datetime    null,
    gmt_create     datetime    null,
    refresh_token  varchar(64) null comment '角色授权',
    alliance_name  varchar(64) null comment '联盟名称',
    alliance_id    int         null comment '联盟ID',
    corp_name      varchar(64) null comment '军团（公司）名称',
    corp_id        int         null comment '军团（公司）ID',
    character_name varchar(64) not null comment '角色名',
    character_id   int         null comment '角色ID',
    user_id        int         not null comment '用户ID',
    id             int auto_increment
        primary key,
    constraint character_character_id_uindex
        unique (character_id),
    constraint character_character_name_uindex
        unique (character_name)
)
    comment '游戏角色表';

create table if not exists industry_job
(
    job_id                bigint      not null comment '作业ID'
        primary key,
    blueprint_id          bigint      null comment '蓝图itemID',
    blueprint_type_id     int         null comment '蓝图invType ID',
    blueprint_type        varchar(64) null comment '蓝图名称',
    cost                  int         null comment '花费',
    licensed_runs         int         null comment '可用流程数',
    probability           decimal     null comment '发明成功几率',
    station_id            bigint      null comment '建筑ID',
    output_location_id    bigint      null comment '产出存放ID',
    product_type          varchar(64) null comment '产出物品类型',
    blueprint_location_id bigint      null comment '蓝图位置ID',
    duration              bigint      null comment '作业时间',
    installer             varchar(64) null comment '启动角色',
    completed_character   varchar(64) null comment '完成角色',
    activity              bigint      null comment '活动ID',
    facility_id           bigint      null comment '作业设施ID',
    runs                  int         null comment '作业次数',
    start_date            datetime    null comment '开始日期',
    end_date              datetime    null comment '结束日期',
    status                varchar(16) null comment 'active-活动 cancelled-取消 delivered-交付 paused-暂停 ready-准备好 reverted-恢复',
    pause_date            datetime    null comment '该建筑服务下线时间',
    installer_id          bigint      null comment '启动角色ID'
)
    comment '制造作业表';

create table if not exists inv_types
(
    type_id                 int              not null
        primary key,
    group_id                int              null,
    type_name               varchar(500)     null,
    description             longtext         null,
    mass                    float            null,
    volume                  float            null,
    packaged_volume         float            null,
    capacity                float            null,
    portion_size            int              null,
    faction_id              int              null,
    race_id                 int unsigned     null,
    base_price              float            null,
    published               tinyint unsigned null,
    market_group_id         int              null,
    graphic_id              int              null,
    radius                  float            null,
    icon_id                 int              null,
    sound_id                int              null,
    sof_faction_name        varchar(100)     null,
    sof_material_set_id     int              null,
    meta_group_id           int              null,
    variationparent_type_id int              null
)
    comment '物品表';

create table if not exists market_groups
(
    market_group_id int              null,
    description_id  mediumtext       null,
    has_types       tinyint unsigned null,
    icon_id         int              null,
    name_id         mediumtext       null,
    parent_group_id int              null
)
    comment '市场组';

create table if not exists market_order
(
    order_id      bigint       not null comment '订单编号'
        primary key,
    duration      bigint       null comment '持续时间',
    is_buy_order  tinyint(1)   null comment '是否买单',
    issued        datetime     null comment '发布时间',
    location_id   bigint       null comment '建筑ID',
    min_volume    bigint       null comment '最小交易数量',
    price         bigint       null comment '价格',
    order_range   varchar(100) null comment '订单范围',
    system_id     bigint       null comment '星系ID',
    type_id       bigint       null comment '物品类型ID',
    volume_remain bigint       null comment '剩余数量',
    volume_total  bigint       null comment '总数量',
    region_id     bigint       null comment '星域ID',
    constraint market_order_price
        unique (order_id, type_id, is_buy_order, price)
);

create table if not exists mining_detail
(
    id                        varchar(32) not null
        primary key,
    character_id              int         null comment '角色ID',
    character_name            varchar(64) null,
    recorded_corporation_id   int         null comment '开采时该角色所属公司',
    recorded_corporation_name varchar(64) null,
    type_id                   int         null comment '物品类型ID',
    quantity                  bigint      null comment '开采数量',
    observer_id               bigint      null comment 'observer id',
    last_updated              date        null comment '上次更新时间'
);

create table if not exists observer
(
    observer_id    bigint      not null,
    observer_type  varchar(64) null comment '观察者类型  枚举：structure ',
    last_updated   datetime    null comment '上次更新时间',
    croporation_id bigint      null,
    constraint observer_observer_id_uindex
        unique (observer_id)
);

alter table observer
    add primary key (observer_id);

create table if not exists sys_menu
(
    id           bigint auto_increment
        primary key,
    name         varchar(64)  default '' null comment '菜单名称',
    parent_id    bigint                  null comment '父菜单ID',
    path         varchar(128) default '' null comment '路由路径',
    component    varchar(128)            null comment '组件路径',
    icon         varchar(64)  default '' null comment '菜单图标',
    sort         int          default 0  null comment '排序',
    visible      tinyint(1)   default 1  null comment '状态：0-禁用 1-开启',
    redirect     varchar(128) default '' null comment '跳转路径',
    gmt_create   datetime                null comment '创建时间',
    gmt_modified datetime                null comment '更新时间'
)
    comment '菜单管理' collate = utf8mb4_general_ci;

create table if not exists sys_permission
(
    id           bigint auto_increment comment '主键'
        primary key,
    name         varchar(64)  null comment '权限名称',
    menu_id      bigint       null comment '菜单模块ID
',
    url_perm     varchar(128) null comment 'URL权限标识',
    btn_perm     varchar(64)  null comment '按钮权限标识',
    gmt_create   datetime     null,
    gmt_modified datetime     null
)
    comment '权限表' collate = utf8mb4_general_ci;

create index id
    on sys_permission (id, name);

create index id_2
    on sys_permission (id, name);

create table if not exists sys_role
(
    id           bigint auto_increment
        primary key,
    name         varchar(64) default '' not null comment '角色名称',
    code         varchar(32)            null comment '角色编码',
    sort         int                    null comment '显示顺序',
    status       tinyint(1)  default 1  null comment '角色状态：0-正常；1-停用',
    deleted      tinyint(1)  default 0  not null comment '逻辑删除标识：0-未删除；1-已删除',
    gmt_create   datetime               null comment '更新时间',
    gmt_modified datetime               null comment '创建时间',
    constraint name
        unique (name)
)
    comment '角色表' collate = utf8mb4_general_ci;

create table if not exists sys_role_menu
(
    role_id      bigint   not null comment '角色ID',
    menu_id      bigint   not null comment '菜单ID',
    gmt_create   datetime null,
    gmt_modified datetime null
)
    comment '角色和菜单关联表' collate = utf8mb4_general_ci;

create table if not exists sys_role_permission
(
    role_id       int      null comment '角色id',
    permission_id int      null comment '资源id',
    gmt_create    datetime null,
    gmt_modified  datetime null
)
    comment '角色权限表' collate = utf8mb4_general_ci;

create index permission_id
    on sys_role_permission (permission_id);

create index role_id
    on sys_role_permission (role_id);

create table if not exists sys_user
(
    id           int auto_increment
        primary key,
    username     varchar(64)             null comment '用户名',
    nickname     varchar(64)             null comment '昵称',
    gender       tinyint(1)   default 1  null comment '性别：1-男 2-女',
    password     varchar(100)            null comment '密码',
    avatar       varchar(255) default '' null comment '用户头像',
    mobile       varchar(20)             null comment '联系方式',
    status       tinyint(1)   default 1  null comment '用户状态：1-正常 0-禁用',
    email        varchar(128)            null comment '用户邮箱',
    deleted      tinyint(1)   default 0  null comment '逻辑删除标识：0-未删除；1-已删除',
    gmt_create   datetime                null comment '创建时间',
    gmt_modified datetime                null comment '更新时间',
    constraint login_name
        unique (username)
)
    comment '用户信息表';

create table if not exists sys_user_role
(
    user_id      int      not null comment '用户ID',
    role_id      int      not null comment '角色ID',
    gmt_create   datetime null,
    gmt_modified datetime null
)
    comment '用户和角色关联表' collate = utf8mb4_general_ci;

create table if not exists universe_name
(
    id       int          not null
        primary key,
    name     varchar(255) null,
    category varchar(64)  null comment 'alliance, character, constellation, corporation, inventory_type, region, solar_system, station, faction'
)
    comment 'universe_name  ids_names';

create table if not exists wallet_journal
(
    owner_id        bigint       null,
    tax_receiver_id int          null,
    tax             double       null,
    second_party_id int          null,
    ref_type        mediumtext   null,
    reason          mediumtext   null,
    first_party_id  int          null,
    description     mediumtext   null,
    date            datetime     null,
    context_id_type varchar(128) null,
    context_id      bigint       null,
    balance         double       null,
    amount          double       null,
    id              bigint       null
);

create index character_wallet_journal_owner_id_index
    on wallet_journal (owner_id);

