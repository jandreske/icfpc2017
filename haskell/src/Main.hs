module Main (main) where

import qualified Data.ByteString.Lazy as B
import System.IO

import Protocol
import State
import Solver


name :: String
name =  "A Storm of Minds: dodo"

main :: IO ()
main =  do handshake
           message <- readWithLength
           appendFile "dodo.log" ("RCV " ++ show message)
           case message of
             (SetupRequest p n map) -> doSetup p n map
             (MoveRequest r state) -> doMove r state

handshake :: IO ()
handshake =  do B.putStr (encodeWithLength (Handshake name))
                hFlush stdout
                response <- readWithLength :: IO Handshake
                appendFile "dodo.log" ("RCV " ++ show response)
                return ()

doSetup :: Int -> Int -> Map -> IO ()
doSetup punter punters map = let state = initState punter punters map in
                             do B.putStr (encodeWithLength (SetupResponse punter state))
                                hFlush stdout

doMove :: Moves -> State -> IO ()
doMove (Moves moves) state =
                     let state' = applyMoves moves state
                         move = generateMove state'
                         state'' = applyMove move state'
                     in
                        do B.putStr (encodeWithLength (adjoin move state''))
                           hFlush stdout

