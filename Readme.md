# EVE-Helper

### 介绍
用户读取自己或者军团的订单资产情况，用于市场分析
### 人物授权
* esi-bookmarks.read_character_bookmarks.v1<br>
  允许读取人物的位标和位标文件夹
* esi-calendar.read_calendar_events.v1<br>
  允许读取人物的日历，包括军团活动
* esi-calendar.respond_calendar_events.v1<br>
  允许更新人物的日历活动回应
* esi-characters.read_agents_research.v1<br>
  允许读取人物的代理人研究状态
* esi-characters.read_blueprints.v1<br>
  允许读取人物的蓝图
* esi-characters.read_corporation_roles.v1<br>
  允许读取人物的军团职位
* esi-characters.read_fatigue.v1<br>
  允许读取人物的跳跃疲劳信息
* esi-characters.read_fw_stats.v1<br>
  允许读取人物的势力战争状态
* esi-characters.read_standings.v1<br>
  允许读取人物的声望
* esi-characters.read_titles.v1<br>
  [待确认]
* esi-fittings.read_fittings.v1<br>
  允许读取装配信息
* esi-fittings.write_fittings.v1<br>
  允许操控装配
* esi-fleets.write_fleet.v1<br>
  允许操控舰队
* esi-industry.read_character_jobs.v1<br>
  允许读取人物的工业项目
* esi-industry.read_character_mining.v1<br>
  允许读取人物的个人采矿活动
* esi-industry.read_corporation_mining.v1<br>
  允许读取和观察军团的采矿活动
* esi-mail.send_mail.v1<br>
  允许为人物发送邮件
* esi-markets.read_character_orders.v1<br>
  允许读取人物的市场订单
* esi-markets.structure_markets.v1<br>
  如果用户拥有某建筑的市场访问权， 允许读取该建筑中的市场数据
* esi-ui.open_window.v1<br>
  允许远程打开游戏客户端中的窗口
* esi-ui.write_waypoint.v1<br>
  允许远程操控游戏客户端中的路径点
  Scopes already granted
* esi-assets.read_assets.v1<br>
  允许读取人物的资产列表
* esi-characters.read_loyalty.v1<br>
  允许读取人物的忠诚点
* esi-characters.read_notifications.v1<br>
  允许读取人物的未决联系人通知
* esi-clones.read_clones.v1<br>
  允许读取人物的远距克隆及其植入体的位置
* esi-clones.read_implants.v1<br>
  允许读取人物当前克隆体的植入体
* esi-contracts.read_character_contracts.v1<br>
  允许读取人物的合同
* esi-fleets.read_fleet.v1<br>
  允许读取舰队信息
* esi-killmails.read_killmails.v1<br>
  允许读取人物的击毁和损失
* esi-location.read_location.v1<br>
  允许读取人物当前驾驶舰船的位置
* esi-location.read_online.v1<br>
  允许读取人物的在线状态
* esi-location.read_ship_type.v1<br>
  允许读取人物当前驾驶舰船的类别
* esi-mail.read_mail.v1<br>
  允许读取人物的收件箱和邮件
* esi-skills.read_skillqueue.v1<br>
  允许读取人物 允许读取人物当前的技能学习队列
* esi-skills.read_skills.v1<br>
  允许读取人物当前已知的技能
* esi-universe.read_structures.v1<br>
  允许读取人物的军团建筑信息
* esi-wallet.read_character_wallet.v1<br>
  允许读取人物的钱包、日志和交易历史

### 军团授权
* esi-assets.read_assets.v1<br>
允许读取人物的资产列表
* esi-assets.read_corporation_assets.v1<br>
允许读取人物军团的资产，前提是该人物有所需的职位
* esi-bookmarks.read_character_bookmarks.v1<br>
允许读取人物的位标和位标文件夹
* esi-bookmarks.read_corporation_bookmarks.v1<br>
允许读取军团的位标和位标文件夹
* esi-characters.read_blueprints.v1<br>
* esi-characters.read_corporation_roles.v1<br>
允许读取人物的军团职位
* esi-contracts.read_character_contracts.v1<br>
允许读取人物的合同
* esi-contracts.read_corporation_contracts.v1<br>
允许读取军团的合同
* esi-corporations.read_blueprints.v1<br>
* esi-corporations.read_divisions.v1<br>
允许读取人物军团的部门名称，前提是该人物有所需的职位
* esi-fittings.write_fittings.v1<br>
允许操控装配
* esi-industry.read_character_jobs.v1<br>
允许读取人物的工业项目
* esi-industry.read_character_mining.v1<br>
允许读取人物的个人采矿活动
* esi-industry.read_corporation_jobs.v1<br>
允许读取人物军团的工业项目，前提是该人物有所需的职位
* esi-killmails.read_killmails.v1<br>
允许读取人物的击毁和损失
* esi-location.read_location.v1<br>
允许读取人物当前驾驶舰船的位置
* esi-location.read_ship_type.v1<br>
允许读取人物当前驾驶舰船的类别
* esi-markets.read_character_orders.v1<br>
允许读取人物的市场订单
* esi-markets.read_corporation_orders.v1<br>
允许读取人物军团的市场订单，前提是该人物有所需的职位
* esi-markets.structure_markets.v1<br>
如果用户拥有某建筑的市场访问权，允许读取该建筑中的市场数据
* esi-planets.manage_planets.v1<br>
允许读取人物的行星开发领地列表及其详细信息
* esi-skills.read_skills.v1<br>
允许读取人物当前已知的技能
* esi-ui.open_window.v1<br>
允许远程打开游戏客户端中的窗口
* esi-ui.write_waypoint.v1<br>
允许远程操控游戏客户端中的路径点
* esi-universe.read_structures.v1<br>
允许读取人物的军团建筑信息
* esi-wallet.read_character_wallet.v1<br>
允许读取人物的钱包、日志和交易历史
* esi-wallet.read_corporation_wallets.v1<br>
允许读取人物军团的钱包、日志和交易记录，前提是该人物有所需的职位

###授权地址
* 设置角色授权的链接地址：
* https://login.evepc.163.com/v2/oauth/authorize?response_type=code&client_id=bc90aa496a404724a93f41b4f4e97761&redirect_uri=https%3A%2F%2Fesi.evepc.163.com%2Fui%2Foauth2-redirect.html&device_id=helper&state=helper&scope=esi-alliances.read_contacts.v1%20esi-assets.read_assets.v1%20esi-assets.read_corporation_assets.v1%20esi-bookmarks.read_character_bookmarks.v1%20esi-bookmarks.read_corporation_bookmarks.v1%20esi-calendar.read_calendar_events.v1%20esi-calendar.respond_calendar_events.v1%20esi-characters.read_agents_research.v1%20esi-characters.read_blueprints.v1%20esi-characters.read_contacts.v1%20esi-characters.read_corporation_roles.v1%20esi-characters.read_fatigue.v1%20esi-characters.read_fw_stats.v1%20esi-characters.read_loyalty.v1%20esi-characters.read_medals.v1%20esi-characters.read_notifications.v1%20esi-characters.read_opportunities.v1%20esi-characters.read_standings.v1%20esi-characters.read_titles.v1%20esi-characters.write_contacts.v1%20esi-clones.read_clones.v1%20esi-clones.read_implants.v1%20esi-contracts.read_character_contracts.v1%20esi-contracts.read_corporation_contracts.v1%20esi-corporations.read_blueprints.v1%20esi-corporations.read_contacts.v1%20esi-corporations.read_container_logs.v1%20esi-corporations.read_corporation_membership.v1%20esi-corporations.read_divisions.v1%20esi-corporations.read_facilities.v1%20esi-corporations.read_fw_stats.v1%20esi-corporations.read_medals.v1%20esi-corporations.read_standings.v1%20esi-corporations.read_starbases.v1%20esi-corporations.read_structures.v1%20esi-corporations.read_titles.v1%20esi-corporations.track_members.v1%20esi-fittings.read_fittings.v1%20esi-fittings.write_fittings.v1%20esi-fleets.read_fleet.v1%20esi-fleets.write_fleet.v1%20esi-industry.read_character_jobs.v1%20esi-industry.read_character_mining.v1%20esi-industry.read_corporation_jobs.v1%20esi-industry.read_corporation_mining.v1%20esi-killmails.read_corporation_killmails.v1%20esi-killmails.read_killmails.v1%20esi-location.read_location.v1%20esi-location.read_online.v1%20esi-location.read_ship_type.v1%20esi-mail.organize_mail.v1%20esi-mail.read_mail.v1%20esi-mail.send_mail.v1%20esi-markets.read_character_orders.v1%20esi-markets.read_corporation_orders.v1%20esi-markets.structure_markets.v1%20esi-planets.manage_planets.v1%20esi-planets.read_customs_offices.v1%20esi-search.search_structures.v1%20esi-skills.read_skillqueue.v1%20esi-skills.read_skills.v1%20esi-ui.open_window.v1%20esi-ui.write_waypoint.v1%20esi-universe.read_structures.v1%20esi-wallet.read_character_wallet.v1%20esi-wallet.read_corporation_wallets.v1


