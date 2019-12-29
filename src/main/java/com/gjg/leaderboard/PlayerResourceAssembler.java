package com.gjg.leaderboard;


import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
class PlayerResourceAssembler implements RepresentationModelAssembler<Player, EntityModel<Player>> {

    @Override
    public EntityModel<Player> toModel(Player player) {

        return new EntityModel<>(player,
                linkTo(methodOn(PlayerController.class).getPlayer(player.getUuid())).withSelfRel(),
                linkTo(methodOn(PlayerController.class).globalLeaderboard()).withRel("players"));
    }
}