package com.ishland.bukkit.carpetplugin.lib.fakeplayer.action;

import com.google.common.base.Preconditions;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FakeEntityPlayerActionPack {

    public final FakeEntityPlayer fakeEntityPlayer;
    private final Set<Action> activeActions = new HashSet<>();

    public FakeEntityPlayerActionPack(FakeEntityPlayer fakeEntityPlayer) {
        Preconditions.checkNotNull(fakeEntityPlayer);
        this.fakeEntityPlayer = fakeEntityPlayer;
    }

    public void tick() {
        Iterator<Action> iterator = activeActions.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            action.tick();
            if (action.isCompleted()) iterator.remove();
        }
    }

    public Set<Action> getActivatedActions() {
        return activeActions;
    }

    public void stopAll() {
        Iterator<Action> iterator = activeActions.iterator();
        while (iterator.hasNext()) {
            Action action = iterator.next();
            action.deactivate();
            iterator.remove();
        }
    }

    public void doSneak() { unSneak(); unSprint(); activeActions.add(new Action(ActionType.SNEAK, fakeEntityPlayer, 5, 1)); }
    public void unSneak() { removeAll(ActionType.SNEAK); }
    public void doSprint() { unSprint(); unSneak(); activeActions.add(new Action(ActionType.SPRINT, fakeEntityPlayer, 5, 1)); }
    public void unSprint() { removeAll(ActionType.SPRINT); }
    public void doUse() { unUse(); activeActions.add(new Action(ActionType.USE, fakeEntityPlayer)); }
    public void doUse(int interval, int repeats) { unUse(); activeActions.add(new Action(ActionType.USE, fakeEntityPlayer, interval, repeats)); }
    public void unUse() { removeAll(ActionType.USE); }
    public void dropOne() { unDrop(); activeActions.add(new Action(ActionType.DROP_ONE, fakeEntityPlayer)); }
    public void dropOne(int interval, int repeats) { unDrop(); activeActions.add(new Action(ActionType.DROP_ONE, fakeEntityPlayer, interval, repeats)); }
    public void dropStack() { unDrop(); activeActions.add(new Action(ActionType.DROP_STACK, fakeEntityPlayer)); }
    public void dropStack(int interval, int repeats) { unDrop(); activeActions.add(new Action(ActionType.DROP_STACK, fakeEntityPlayer, interval, repeats)); }
    public void dropAll() { unDrop(); activeActions.add(new Action(ActionType.DROP_ALL, fakeEntityPlayer)); }
    public void dropAll(int interval, int repeats) { unDrop(); activeActions.add(new Action(ActionType.DROP_ALL, fakeEntityPlayer, interval, repeats)); }
    public void unDrop() { removeAll(ActionType.DROP_ALL); removeAll(ActionType.DROP_ONE); removeAll(ActionType.DROP_STACK); }

    private void removeAll(ActionType type) {
        activeActions.removeIf(action -> {
            if (action.actionType == type) { action.deactivate(); return true; }
            return false;
        });
    }

    public enum ActionType {
        SNEAK {
            @Override public void tick(FakeEntityPlayer p) { if (!p.isShiftKeyDown()) p.setShiftKeyDown(true); }
            @Override public void deactivate(FakeEntityPlayer p) { p.setShiftKeyDown(false); }
        },
        SPRINT {
            @Override public void tick(FakeEntityPlayer p) { if (!p.isSprinting()) p.setSprinting(true); }
            @Override public void deactivate(FakeEntityPlayer p) { p.setSprinting(false); }
        },
        USE {
            @Override
            public void tick(FakeEntityPlayer player) {
                HitResult hit = player.pick(5.0, 0f, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    Direction dir = blockHit.getDirection();
                    Level world = player.level();
                    if (pos.getY() < player.server.getMaxBuildHeight() - (dir == Direction.UP ? 1 : 0)
                            && world.mayInteract(player, pos)) {
                        for (InteractionHand hand : InteractionHand.values()) {
                            InteractionResult result = player.gameMode.useItemOn(
                                    player, world, player.getItemInHand(hand), hand, blockHit);
                            if (result.consumesAction()) {
                                player.swing(hand);
                                return;
                            }
                        }
                    }
                } else if (hit instanceof EntityHitResult entityHit) {
                    Entity entity = entityHit.getEntity();
                    Vec3 relPos = entityHit.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
                    for (InteractionHand hand : InteractionHand.values()) {
                        if (entity.interactAt(player, relPos, hand).consumesAction()) return;
                        if (player.interactOn(entity, hand).consumesAction()) return;
                    }
                }
                for (InteractionHand hand : InteractionHand.values()) {
                    if (player.gameMode.useItem(player, player.level(), player.getItemInHand(hand), hand).consumesAction())
                        return;
                }
            }
            @Override public void deactivate(FakeEntityPlayer p) { p.releaseUsingItem(); }
        },
        DROP_ONE {
            @Override public void tick(FakeEntityPlayer p) { p.drop(false); }
            @Override public void deactivate(FakeEntityPlayer p) {}
        },
        DROP_STACK {
            @Override public void tick(FakeEntityPlayer p) { p.drop(true); }
            @Override public void deactivate(FakeEntityPlayer p) {}
        },
        DROP_ALL {
            @Override
            public void tick(FakeEntityPlayer p) {
                dropList(p, p.getInventory().armor);
                dropList(p, p.getInventory().items);
                dropList(p, p.getInventory().offhand);
            }
            private void dropList(FakeEntityPlayer player, List<ItemStack> list) {
                for (int i = 0; i < list.size(); i++) {
                    ItemStack stack = list.get(i);
                    if (!stack.isEmpty()) {
                        player.drop(stack, false, true);
                        list.set(i, ItemStack.EMPTY);
                    }
                }
            }
            @Override public void deactivate(FakeEntityPlayer p) {}
        };

        public abstract void tick(FakeEntityPlayer player);
        public abstract void deactivate(FakeEntityPlayer player);
    }

    public class Action {
        public final ActionType actionType;
        public final FakeEntityPlayer player;
        public final boolean isOnce;
        public final int interval;
        public final int repeats;
        private boolean isExecuted = false;
        private long ticks = -1L;

        public Action(ActionType actionType, FakeEntityPlayer player) {
            this(actionType, player, 1, 1);
        }

        public Action(ActionType actionType, FakeEntityPlayer player, int interval, int repeats) {
            Preconditions.checkNotNull(actionType);
            Preconditions.checkNotNull(player);
            Preconditions.checkArgument(interval > 0);
            Preconditions.checkArgument(repeats > 0);
            this.actionType = actionType;
            this.player = player;
            this.isOnce = (interval == 1 && repeats == 1);
            this.interval = interval;
            this.repeats = repeats;
        }

        public void tick() {
            ticks++;
            if (isOnce && isExecuted) return;
            if (ticks % interval == 0) {
                isExecuted = true;
                for (int i = 0; i < repeats; i++) actionType.tick(player);
            }
        }

        public void deactivate() { actionType.deactivate(player); }
        public boolean isCompleted() { return isOnce && isExecuted; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Action action = (Action) o;
            return isOnce == action.isOnce && interval == action.interval && repeats == action.repeats
                    && actionType == action.actionType && player.equals(action.player);
        }

        @Override
        public int hashCode() { return Objects.hash(actionType, player, isOnce, interval, repeats); }

        @Override
        public String toString() {
            return "Action{type=" + actionType + ", once=" + isOnce + ", interval=" + interval + ", repeats=" + repeats + "}";
        }
    }
}
