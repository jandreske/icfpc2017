module Solver (generateMove) where

import Data.List

import Protocol
import State

generateMove :: State -> Move
generateMove state = makeClaim (getPunterId state) best
  where
    candidates = getUnclaimedRivers state
    ((_,best):_) = sort [(priority state river, river) | river <- candidates]

priority :: State -> River -> Int
priority state river
    | incidentToMine state river =
          if isUnclaimedMine state (riverSource river) || isUnclaimedMine state (riverTarget river)
           then 0 else 1
    | connected state river = 2
    | otherwise = 3

isUnclaimedMine :: State -> Int -> Bool
isUnclaimedMine state site = isMine state site && not (siteClaimed state site)
