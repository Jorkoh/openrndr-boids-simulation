# Boids simulation with OPENRNDR

[![Demo on YouTube](https://i.imgur.com/d6VM9fb.png)](https://www.youtube.com/watch?v=6SdYMsDuIJg "Demo on YouTube")

Classic [boids simulation](https://www.red3d.com/cwr/boids/) where each agent is only aware of other agents in close proximity (range shown at 0:40) and follows the three basic rules first defined by Craig Reynolds:

- Separation, steer to avoid crowding local agents
- Alignment, steer towards the average heading of local agents
- Cohesion, steer to move towards the average position (center of mass) of local agents

Aditionally the agents avoid walls, and predators (on their vision range). The predators simply avoid walls, other predators and chase the closest boid (on their vision range).

Performance is improved by using a [**quadtree**](https://en.wikipedia.org/wiki/Quadtree) for [spatial partitioning](https://gameprogrammingpatterns.com/spatial-partition.html) (shown at 0:40). This way we don't need to check the distance of a boid against every other boid to determine which ones are in his interaction range. The data structure dynamically organizes the objects by their positions and the query is much more efficient. The quadtree divides the space aiming to keep a maximum amount of agents on each region, in the video the regions turn red as they reach max capacity.

Aditionally **coroutines** are used to parallelize the calculation of the interactions between the agents and the updates of the quadtree. Finally the angle of the velocity vector of the agents is cached since it needs to be used few times each update.

The simulation is completely independent from the rendering so it's easy to run the simulation headless or change the graphics layer to other engine. Currently using [OPENRNDR](https://github.com/openrndr/openrndr) to draw the boids and it has worked pretty well.

Note: the bucket size of the quadtree shown on the demo is not the most optimal but the subdivision of the quadtree is more visible with smaller bucket sizes like the one used.
