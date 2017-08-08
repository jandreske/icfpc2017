{-# LANGUAGE BangPatterns #-}
module Paths (shortestPathLength, canMove) where

import qualified Data.IntMap.Strict as IM
import qualified Queue as Q
import Data.Maybe

import Protocol
import State

shortestPathLength :: [River] -> Int -> Int -> Maybe Int
shortestPathLength rivers source target = go visited0 queue0
  where
    visited0 = IM.singleton source Nothing
    queue0 = Q.singleton source
    go visited queue =
      case Q.take queue of
        Nothing -> Nothing
        Just (node,queue') | node == target -> traceback visited node 0
                           | otherwise      -> enqueue node visited queue rivers
    enqueue _    visited queue [] = go visited queue
    enqueue node visited queue (r:rs)
      | riverSource r == node && IM.notMember (riverTarget r) visited =
                                   enqueue node (IM.insert (riverTarget r) node visited)
                                                (Q.put (riverTarget r) queue)
                                                rs
      | riverTarget r == node && IM.notMember (riverSource r) visited =
                                   enqueue node (IM.insert (riverSource r) node visited)
                                                (Q.put (riverSource r) queue)
                                                rs
      | otherwise =                enqueue node visited queue rs
    traceback visited node !len = case IM.lookup node visited of
                                    Nothing -> Just len
                                    Just prev -> traceback visited prev (len + 1)

canMove :: State -> Int -> Int -> Bool
canMove state a b = isJust (shortestPathLength (getOwnRivers state) a b)
