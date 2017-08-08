module Queue (Queue, null, empty, singleton, take, put) where

import Prelude hiding (null, take)

data Queue a = Queue [a] [a]

null :: Queue a -> Bool
null (Queue [] []) = True
null _             = False

empty :: Queue a
empty = Queue [] []

singleton :: a -> Queue a
singleton x = Queue [x] []

put :: a -> Queue a -> Queue a
put x (Queue hd tl) = Queue hd (x:tl)

size :: Queue a -> Int
size (Queue hd tl) = length hd + length tl

take (Queue hd tl) = case hd of
                       (x:xs) -> Just (x, Queue xs tl)
                       [] -> case tl of
                               [] -> Nothing
                               _  -> let (y:ys) = reverse tl in
                                     Just (y, Queue ys [])
