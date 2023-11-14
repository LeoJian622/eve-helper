package xyz.foolcat.eve.evehelper.esi.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import xyz.foolcat.eve.evehelper.esi.model.sub.Link;
import xyz.foolcat.eve.evehelper.esi.model.sub.Pin;
import xyz.foolcat.eve.evehelper.esi.model.sub.Route;

import java.util.List;

/**
 * 行星殖民地布局的全部详细信息，包括链接、插针和路线
 *
 * @author Leojan
 * date 2023-11-07 14:32
 */

@Data
@Tag(name = "行星殖民地布局详细信息  200 ok")
public class ColonyLayoutResponse {

    private List<Link> links;

    private List<Pin> pins;

    private List<Route> routes;
}
