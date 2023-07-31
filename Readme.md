# EVE-Helper

### 介绍
用户读取自己或者军团的订单资产情况，用于市场分析
### 人物授权
* 允许读取人物的资产列表<br>esi-assets.read_assets.v1
* 允许读取人物的蓝图列表<br>esi-characters.read_blueprints.v1
* 允许读取人物的工业项目<br>esi-industry.read_character_jobs.v1
* 允许读取人物的个人采矿活动<br>esi-industry.read_character_mining.v1
* 允许读取人物的市场订单<br>esi-markets.read_character_orders.v1
* 如果用户拥有某建筑的市场访问权，允许读取该建筑中的市场数据<br>esi-markets.structure_markets.v1
* 允许读取人物的钱包、日志和交易历史<br>esi-wallet.read_character_wallet.v1

### 技能读取
* 允许读取人物的技能<br>esi-skills.read_skills.v1
* 允许读取人物的技能队列<br>esi-skills.read_skillqueue.v1



### 军团授权
* 允许读取军团的资产列表<br>esi-assets.read_corporation_assets.v1
* 允许读取军团的蓝图列表<br>esi-corporations.read_blueprints.v1 
* 允许读取军团的工业项目<br>esi-industry.read_corporation_jobs.v1
* 允许读取军团的个人采矿活动<br>esi-industry.read_corporation_mining.v1
* 允许读取军团的市场订单<br>esi-markets.read_corporation_orders.v1
* 允许读取军团的市场订单<br>esi-wallet.read_corporation_wallets.v1
* 允许读取军团的建筑<br>esi-corporations.read_structures.v1


### 通用授权
* 允许读取宇宙通用信息<br>esi-universe.read_structures.v1

###授权地址
* 设置角色授权的链接地址：
* https://login.evepc.163.com/v2/oauth/authorize?response_type=code&redirect_uri=https%3A%2F%2Fesi.evepc.163.com%2Fui%2Foauth2-redirect.html&client_id=bc90aa496a404724a93f41b4f4e97761&scope=esi-assets.read_assets.v1+esi-characters.read_blueprints.v1+esi-industry.read_character_jobs.v1+esi-industry.read_character_mining.v1+esi-markets.read_character_orders.v1+esi-markets.structure_markets.v1+esi-wallet.read_character_wallet.v1&state=T&device_id=T

* 设置军团授权的链接地址：
* https://login.evepc.163.com/v2/oauth/authorize?response_type=code&redirect_uri=https%3A%2F%2Fesi.evepc.163.com%2Fui%2Foauth2-redirect.html&client_id=bc90aa496a404724a93f41b4f4e97761&scope=esi-assets.read_corporation_assets.v1+esi-corporations.read_blueprints.v1+esi-industry.read_corporation_jobs.v1+esi-industry.read_corporation_mining.v1+esi-markets.read_corporation_orders.v1+esi-wallet.read_corporation_wallets.v1&state=T&device_id=T


* 角色技能：
* https://login.evepc.163.com/v2/oauth/authorize?response_type=code&redirect_uri=https%3A%2F%2Fesi.evepc.163.com%2Fui%2Foauth2-redirect.html&client_id=bc90aa496a404724a93f41b4f4e97761&scope=esi-skills.read_skillqueue.v1+esi-skills.read_skills.v1&state=T&device_id=T


* 通用授权：
* https://login.evepc.163.com/v2/oauth/authorize?response_type=code&redirect_uri=https%3A%2F%2Fesi.evepc.163.com%2Fui%2Foauth2-redirect.html&client_id=bc90aa496a404724a93f41b4f4e97761&scope=esi-ui.open_window.v1+esi-ui.write_waypoint.v1+esi-universe.read_structures.v1+&state=T&device_id=T


* 采矿明细授权：
* https://login.evepc.163.com/v2/oauth/authorize?response_type=code&redirect_uri=https%3A%2F%2Fesi.evepc.163.com%2Fui%2Foauth2-redirect.html&client_id=bc90aa496a404724a93f41b4f4e97761&scope=esi-corporations.read_structures.v1+esi-industry.read_corporation_mining.v1&state=T&device_id=T