{-# LANGUAGE OverloadedStrings #-}
module Protocol where

import Data.Aeson
import Data.Foldable
import qualified Data.ByteString.Lazy as B
import qualified Data.ByteString.Lazy.Char8 as BC
import System.IO


-- The task requires JSON messages to be prefixed with a
-- length in characters, like this:
--    7:{"x"=0}
-- Assuming we use only ASCII characters, the length can
-- treated as the number of bytes.

-- Encode value as length-prefixed JSON
encodeWithLength :: (ToJSON a) => a -> B.ByteString
encodeWithLength x = let s = encode x
                         n = B.length s
                     in
                         B.append (BC.pack (shows n ":")) s

-- decode length-prefixed JSON value
decodeWithLength :: (FromJSON a) => B.ByteString -> Maybe (a, B.ByteString)
decodeWithLength s = let (n',r') = BC.span (':' /=) s
                         n = read (BC.unpack n')
                         r = B.tail r'
                         (o,s') = B.splitAt n r
                     in
                        case decode o of
                          Nothing -> Nothing
                          Just v -> Just (v,s')

readWithLength :: (FromJSON a) => IO a
readWithLength = do n <- readInt
                    s <- B.hGet stdin n
                    case eitherDecode s of
                      Left err -> fail err
                      Right value -> return value

readInt :: IO Int
readInt = go 0
  where
    go n = do c <- getChar
              if c == ':' then return n
                else go (10 * n + fromEnum c - 48)


newtype Handshake = Handshake String deriving (Show)

instance FromJSON Handshake where
  parseJSON = withObject "handshake" $ \o ->
    Handshake <$> o .: "you"

instance ToJSON Handshake where
  toJSON (Handshake name) = object [ "me" .= name ]

data Site = Site { siteId :: !Int, siteX, siteY :: !Double }

instance Eq Site where
  a == b  = siteId a == siteId b

instance Ord Site where
  compare a b = compare (siteId a) (siteId b)

instance Show Site where
  showsPrec _ s = shows (siteId s)

instance FromJSON Site where
  parseJSON = withObject "site" $ \o ->
    Site <$> o .: "id" <*> o .: "x" <*> o .: "y"

instance ToJSON Site where
  toJSON s = object [ "id" .= siteId s, "x" .= siteX s, "y" .= siteY s ]

data River = River { riverSource, riverTarget :: !Int }
           deriving (Eq, Ord)

makeRiver :: Int -> Int -> River
makeRiver a b | a <= b    = River a b
              | otherwise = River b a

instance Show River where
  showsPrec _ river = shows (riverSource river) . showChar '-' . shows (riverTarget river)

instance FromJSON River where
  parseJSON = withObject "river" $ \o ->
    makeRiver <$> o .: "source" <*> o .: "target"

instance ToJSON River where
  toJSON r = object [ "source" .= riverSource r, "target" .= riverTarget r ]

data Map = Map { mapSites :: [Site], mapRivers :: [River], mapMines :: [Int] }
         deriving (Show)

instance FromJSON Map where
  parseJSON = withObject "map" $ \o ->
    Map <$> o .: "sites" <*> o .: "rivers" <*> o .: "mines"

instance ToJSON Map where
  toJSON (Map s r m) = object [ "sites" .= s, "rivers" .= r, "mines" .= m ]

data SetupResponse state = SetupResponse { srReady :: !Int, srState :: state }
                         deriving (Show)

instance ToJSON state => ToJSON (SetupResponse state) where
  toJSON r = object [ "ready" .= srReady r, "state" .= srState r ]

data Move = Claim !ClaimData
          | Pass !PassData
          deriving (Show)

data Move' state = Claim' !ClaimData state
                 | Pass' !PassData state

data ClaimData = ClaimData !Int !Int !Int deriving (Show)
data PassData = PassData !Int deriving (Show)

makeClaim :: Int -> River -> Move
makeClaim punterId river = Claim (ClaimData punterId (riverSource river) (riverTarget river))

instance ToJSON ClaimData where
  toJSON (ClaimData p s t) = object [ "punter" .= p, "source" .= s, "target" .= t ]

instance ToJSON PassData where
  toJSON (PassData p) = object [ "punter" .= p ]

instance FromJSON ClaimData where
  parseJSON = withObject "claim" $ \o ->
    ClaimData <$> o .: "punter" <*> o .: "source" <*> o .: "target"

instance FromJSON PassData where
  parseJSON = withObject "pass" $ \o ->
    PassData <$> o .: "punter"


instance ToJSON Move where
  toJSON (Claim d) = object [ "claim" .= d ]
  toJSON (Pass d)  = object [ "pass" .= d ]

instance FromJSON Move where
  parseJSON = withObject "move" $ \o -> asum [
    Claim <$> o .: "claim",
    Pass <$> o .: "pass" ]

instance (ToJSON state) => ToJSON (Move' state) where
  toJSON (Claim' d s) = object [ "claim" .= d, "state" .= s ]
  toJSON (Pass' d s)  = object [ "pass" .= d, "state" .= s ]

instance (FromJSON state) => FromJSON (Move' state) where
  parseJSON = withObject "move" $ \o -> asum [
    Claim' <$> o .: "claim" <*> o .: "state",
    Pass' <$> o .: "pass" <*> o .: "state" ]

data Moves = Moves [Move] deriving (Show)

instance ToJSON Moves where
  toJSON (Moves moves) = object [ "moves" .= moves ]

instance FromJSON Moves where
  parseJSON = withObject "moves" $ \o ->
    Moves <$> o .: "moves"

data OfflineRequest state = SetupRequest { sqPunter, sqPunters :: !Int, sqMap :: Map }
                          | MoveRequest Moves state
                          deriving (Show)

instance (FromJSON state) => FromJSON (OfflineRequest state) where
  parseJSON = withObject "setup or moves" $ \o -> asum [
    SetupRequest <$> o .: "punter" <*> o .: "punters" <*> o .: "map",
    MoveRequest <$> o .: "move" <*> o .: "state" ]

adjoin :: Move -> state -> Move' state
adjoin (Claim d) s = Claim' d s
adjoin (Pass d)  s = Pass' d s
