package xyz.foolcat.eve.evehelper.esi.auth;

import java.util.*;

/**
 * @author Leojan
 * date 2023-08-01 16:08
 */

public interface SsoScopes {
    String PUBLIC_DATA = "publicData";
    String ESI_ALLIANCES_READ_CONTACTS_V1 = "esi-alliances.read_contacts.v1";
    String ESI_ASSETS_READ_ASSETS_V1 = "esi-assets.read_assets.v1";
    String ESI_ASSETS_READ_CORPORATION_ASSETS_V1 = "esi-assets.read_corporation_assets.v1";
    String ESI_BOOKMARKS_READ_CHARACTER_BOOKMARKS_V1 = "esi-bookmarks.read_character_bookmarks.v1";
    String ESI_BOOKMARKS_READ_CORPORATION_BOOKMARKS_V1 = "esi-bookmarks.read_corporation_bookmarks.v1";
    String ESI_CALENDAR_READ_CALENDAR_EVENTS_V1 = "esi-calendar.read_calendar_events.v1";
    String ESI_CALENDAR_RESPOND_CALENDAR_EVENTS_V1 = "esi-calendar.respond_calendar_events.v1";
    String ESI_CHARACTERS_READ_AGENTS_RESEARCH_V1 = "esi-characters.read_agents_research.v1";
    String ESI_CHARACTERS_READ_BLUEPRINTS_V1 = "esi-characters.read_blueprints.v1";
    String ESI_CHARACTERS_READ_CONTACTS_V1 = "esi-characters.read_contacts.v1";
    String ESI_CHARACTERS_READ_CORPORATION_ROLES_V1 = "esi-characters.read_corporation_roles.v1";
    String ESI_CHARACTERS_READ_FATIGUE_V1 = "esi-characters.read_fatigue.v1";
    String ESI_CHARACTERS_READ_FW_STATS_V1 = "esi-characters.read_fw_stats.v1";
    String ESI_CHARACTERS_READ_LOYALTY_V1 = "esi-characters.read_loyalty.v1";
    String ESI_CHARACTERS_READ_MEDALS_V1 = "esi-characters.read_medals.v1";
    String ESI_CHARACTERS_READ_NOTIFICATIONS_V1 = "esi-characters.read_notifications.v1";
    String ESI_CHARACTERS_READ_OPPORTUNITIES_V1 = "esi-characters.read_opportunities.v1";
    String ESI_CHARACTERS_READ_STANDINGS_V1 = "esi-characters.read_standings.v1";
    String ESI_CHARACTERS_READ_TITLES_V1 = "esi-characters.read_titles.v1";
    String ESI_CHARACTERS_WRITE_CONTACTS_V1 = "esi-characters.write_contacts.v1";
    String ESI_CLONES_READ_CLONES_V1 = "esi-clones.read_clones.v1";
    String ESI_CLONES_READ_IMPLANTS_V1 = "esi-clones.read_implants.v1";
    String ESI_CONTRACTS_READ_CHARACTER_CONTRACTS_V1 = "esi-contracts.read_character_contracts.v1";
    String ESI_CONTRACTS_READ_CORPORATION_CONTRACTS_V1 = "esi-contracts.read_corporation_contracts.v1";
    String ESI_CORPORATIONS_READ_BLUEPRINTS_V1 = "esi-corporations.read_blueprints.v1";
    String ESI_CORPORATIONS_READ_CONTACTS_V1 = "esi-corporations.read_contacts.v1";
    String ESI_CORPORATIONS_READ_CONTAINER_LOGS_V1 = "esi-corporations.read_container_logs.v1";
    String ESI_CORPORATIONS_READ_CORPORATION_MEMBERSHIP_V1 = "esi-corporations.read_corporation_membership.v1";
    String ESI_CORPORATIONS_READ_DIVISIONS_V1 = "esi-corporations.read_divisions.v1";
    String ESI_CORPORATIONS_READ_FACILITIES_V1 = "esi-corporations.read_facilities.v1";
    String ESI_CORPORATIONS_READ_FW_STATS_V1 = "esi-corporations.read_fw_stats.v1";
    String ESI_CORPORATIONS_READ_MEDALS_V1 = "esi-corporations.read_medals.v1";
    String ESI_CORPORATIONS_READ_STANDINGS_V1 = "esi-corporations.read_standings.v1";
    String ESI_CORPORATIONS_READ_STARBASES_V1 = "esi-corporations.read_starbases.v1";
    String ESI_CORPORATIONS_READ_STRUCTURES_V1 = "esi-corporations.read_structures.v1";
    String ESI_CORPORATIONS_READ_TITLES_V1 = "esi-corporations.read_titles.v1";
    String ESI_CORPORATIONS_TRACK_MEMBERS_V1 = "esi-corporations.track_members.v1";
    String ESI_FITTINGS_READ_FITTINGS_V1 = "esi-fittings.read_fittings.v1";
    String ESI_FITTINGS_WRITE_FITTINGS_V1 = "esi-fittings.write_fittings.v1";
    String ESI_FLEETS_READ_FLEET_V1 = "esi-fleets.read_fleet.v1";
    String ESI_FLEETS_WRITE_FLEET_V1 = "esi-fleets.write_fleet.v1";
    String ESI_INDUSTRY_READ_CHARACTER_JOBS_V1 = "esi-industry.read_character_jobs.v1";
    String ESI_INDUSTRY_READ_CHARACTER_MINING_V1 = "esi-industry.read_character_mining.v1";
    String ESI_INDUSTRY_READ_CORPORATION_JOBS_V1 = "esi-industry.read_corporation_jobs.v1";
    String ESI_INDUSTRY_READ_CORPORATION_MINING_V1 = "esi-industry.read_corporation_mining.v1";
    String ESI_KILLMAILS_READ_CORPORATION_KILLMAILS_V1 = "esi-killmails.read_corporation_killmails.v1";
    String ESI_KILLMAILS_READ_KILLMAILS_V1 = "esi-killmails.read_killmails.v1";
    String ESI_LOCATION_READ_LOCATION_V1 = "esi-location.read_location.v1";
    String ESI_LOCATION_READ_ONLINE_V1 = "esi-location.read_online.v1";
    String ESI_LOCATION_READ_SHIP_TYPE_V1 = "esi-location.read_ship_type.v1";
    String ESI_MAIL_ORGANIZE_MAIL_V1 = "esi-mail.organize_mail.v1";
    String ESI_MAIL_READ_MAIL_V1 = "esi-mail.read_mail.v1";
    String ESI_MAIL_SEND_MAIL_V1 = "esi-mail.send_mail.v1";
    String ESI_MARKETS_READ_CHARACTER_ORDERS_V1 = "esi-markets.read_character_orders.v1";
    String ESI_MARKETS_READ_CORPORATION_ORDERS_V1 = "esi-markets.read_corporation_orders.v1";
    String ESI_MARKETS_STRUCTURE_MARKETS_V1 = "esi-markets.structure_markets.v1";
    String ESI_PLANETS_MANAGE_PLANETS_V1 = "esi-planets.manage_planets.v1";
    String ESI_PLANETS_READ_CUSTOMS_OFFICES_V1 = "esi-planets.read_customs_offices.v1";
    String ESI_SEARCH_SEARCH_STRUCTURES_V1 = "esi-search.search_structures.v1";
    String ESI_SKILLS_READ_SKILLQUEUE_V1 = "esi-skills.read_skillqueue.v1";
    String ESI_SKILLS_READ_SKILLS_V1 = "esi-skills.read_skills.v1";
    String ESI_UI_OPEN_WINDOW_V1 = "esi-ui.open_window.v1";
    String ESI_UI_WRITE_WAYPOINT_V1 = "esi-ui.write_waypoint.v1";
    String ESI_UNIVERSE_READ_STRUCTURES_V1 = "esi-universe.read_structures.v1";
    String ESI_WALLET_READ_CHARACTER_WALLET_V1 = "esi-wallet.read_character_wallet.v1";
    String ESI_WALLET_READ_CORPORATION_WALLETS_V1 = "esi-wallet.read_corporation_wallets.v1";

    Set<String> ALL = Set.of("esi-alliances.read_contacts.v1", "esi-assets.read_assets.v1", "esi-assets.read_corporation_assets.v1", "esi-bookmarks.read_character_bookmarks.v1", "esi-bookmarks.read_corporation_bookmarks.v1", "esi-calendar.read_calendar_events.v1", "esi-calendar.respond_calendar_events.v1", "esi-characters.read_agents_research.v1", "esi-characters.read_blueprints.v1", "esi-characters.read_contacts.v1", "esi-characters.read_corporation_roles.v1", "esi-characters.read_fatigue.v1", "esi-characters.read_fw_stats.v1", "esi-characters.read_loyalty.v1", "esi-characters.read_medals.v1", "esi-characters.read_notifications.v1", "esi-characters.read_opportunities.v1", "esi-characters.read_standings.v1", "esi-characters.read_titles.v1", "esi-characters.write_contacts.v1", "esi-clones.read_clones.v1", "esi-clones.read_implants.v1", "esi-contracts.read_character_contracts.v1", "esi-contracts.read_corporation_contracts.v1", "esi-corporations.read_blueprints.v1", "esi-corporations.read_contacts.v1", "esi-corporations.read_container_logs.v1", "esi-corporations.read_corporation_membership.v1", "esi-corporations.read_divisions.v1", "esi-corporations.read_facilities.v1", "esi-corporations.read_fw_stats.v1", "esi-corporations.read_medals.v1", "esi-corporations.read_standings.v1", "esi-corporations.read_starbases.v1", "esi-corporations.read_structures.v1", "esi-corporations.read_titles.v1", "esi-corporations.track_members.v1", "esi-fittings.read_fittings.v1", "esi-fittings.write_fittings.v1", "esi-fleets.read_fleet.v1", "esi-fleets.write_fleet.v1", "esi-industry.read_character_jobs.v1", "esi-industry.read_character_mining.v1", "esi-industry.read_corporation_jobs.v1", "esi-industry.read_corporation_mining.v1", "esi-killmails.read_corporation_killmails.v1", "esi-killmails.read_killmails.v1", "esi-location.read_location.v1", "esi-location.read_online.v1", "esi-location.read_ship_type.v1", "esi-mail.organize_mail.v1", "esi-mail.read_mail.v1", "esi-mail.send_mail.v1", "esi-markets.read_character_orders.v1", "esi-markets.read_corporation_orders.v1", "esi-markets.structure_markets.v1", "esi-planets.manage_planets.v1", "esi-planets.read_customs_offices.v1", "esi-search.search_structures.v1", "esi-skills.read_skillqueue.v1", "esi-skills.read_skills.v1", "esi-ui.open_window.v1", "esi-ui.write_waypoint.v1", "esi-universe.read_structures.v1", "esi-wallet.read_character_wallet.v1", "esi-wallet.read_corporation_wallets.v1");
}
