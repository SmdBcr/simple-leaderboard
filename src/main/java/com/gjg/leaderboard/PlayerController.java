package com.gjg.leaderboard;


import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
class PlayerController {

    private final PlayerRepository repository;
    private final PlayerResourceAssembler assembler;


    PlayerController(PlayerRepository repository,
                     PlayerResourceAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    // Aggregate root

    @GetMapping("/leaderboard")
    CollectionModel<EntityModel<Player>> globalLeaderboard() {

        List<EntityModel<Player>> players = repository.findAll().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return new CollectionModel<>(players,
                linkTo(methodOn(PlayerController.class).globalLeaderboard()).withSelfRel());
    }

    @GetMapping("/leaderboard/{country-iso-code}")
    CollectionModel<EntityModel<Player>> countryLeaderboard() {
/*
        List<EntityModel<Player>> players = repository.findAll().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return new CollectionModel<>(players,
                linkTo(methodOn(PlayerController.class).all()).withSelfRel());*/
        return null;
        //todo implement
    }


    @PostMapping("/score/submit")
    ScoreSubmission newScoreSubmission(@RequestBody ScoreSubmission scoreSubmission) {
        //todo update score if higher than last one
        return null;
    }

    // Single item
    @GetMapping("/user/profile/{id}")
    EntityModel<Player> getPlayer(@PathVariable UUID id) {

        Player player = repository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));

        return assembler.toModel(player);
/*        return new EntityModel<>(player,
                linkTo(methodOn(PlayerController.class).one(id)).withSelfRel(),
                linkTo(methodOn(PlayerController.class).all()).withRel("player"));*/
    }


    @PutMapping("/user/profile/{id}")
    Player editPlayer(@RequestBody Player player, @PathVariable UUID id) {
        //todo update display name and country ??
        return repository.findById(id)
                .map(player1 -> {
                    player1.setName(player.getName());
                    player1.setCountry(player.getCountry());
                    return repository.save(player1);
                })
                .orElseGet(() -> {
                    player.setUuid(id);
                    return repository.save(player);
                });
    }

    @DeleteMapping("/user/profile/delete")
    ResponseEntity<?> deletePlayer(@RequestBody Player player) {
        repository.deleteById(player.getUuid());
        return ResponseEntity.noContent().build();
    }

}