module State where

import Protocol

type State = (Int, Int, Map, [(River, Maybe Int)])

getPunterId :: State -> Int
getPunterId (p,_,_,_) = p

getNumPunters :: State -> Int
getNumPunters (_,n,_,_) = n

getMap :: State -> Map
getMap (_,_,m,_) = m

getRivers :: State -> [(River, Maybe Int)]
getRivers (_,_,_,r) = r

getUnclaimedRivers :: State -> [River]
getUnclaimedRivers state = [river | (river, Nothing) <- getRivers state]

getOwnRivers :: State -> [River]
getOwnRivers state = [river | (river, Just p) <- getRivers state, p == getPunterId state]


initState :: Int -> Int -> Map -> State
initState p n m = (p,n,m,[(river,Nothing) | river <- mapRivers m])

applyMove :: Move -> State -> State
applyMove (Pass _) state = state
applyMove (Claim (ClaimData p s t)) (pu,n,m,r) =
  (pu,n,m, [(ri, if ri == river then Just p else own) | (ri,own) <- r])
  where river = makeRiver s t

applyMoves :: [Move] -> State -> State
applyMoves ms s = foldr applyMove s ms


incident :: River -> Int -> Bool
incident river site = site == riverSource river || site == riverTarget river

isMine :: State -> Int -> Bool
isMine state site = site `elem` mapMines (getMap state)

incidentToMine :: State -> River -> Bool
incidentToMine state river = not (null [m | m <- mapMines (getMap state), incident river m])

connected :: State -> River -> Bool
connected state river = siteClaimed state (riverSource river)
                        || siteClaimed state (riverTarget river)

siteClaimed :: State -> Int -> Bool
siteClaimed state site = any (\r -> incident r site) (getOwnRivers state)
