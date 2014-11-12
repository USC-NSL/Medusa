-- MySQL dump 10.13  Distrib 5.1.63, for debian-linux-gnu (i486)
--
-- Host: xxx.xxx.xxx.xxx    Database: medusa
-- ------------------------------------------------------
-- Server version	5.0.75-0ubuntu10.5

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP DATABASE IF EXISTS `medusa`;
CREATE DATABASE IF NOT EXISTS `medusa`;
use medusa;

--
-- Not dumping tablespaces as no INFORMATION_SCHEMA.FILES table on this server
--

--
-- Table structure for table `CS_app2pid`
--

DROP TABLE IF EXISTS `CS_app2pid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_app2pid` (
  `appname` varchar(64) NOT NULL,
  `pid` varchar(32) NOT NULL,
  `timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CS_app2pid`
--

--
-- Table structure for table `CS_c2dm`
--

DROP TABLE IF EXISTS `CS_c2dm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_c2dm` (
  `wid` varchar(128) NOT NULL,
  `regid` varchar(1024) NOT NULL,
  `timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CS_cmdqueue`
--

DROP TABLE IF EXISTS `CS_cmdqueue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_cmdqueue` (
  `cid` int(11) NOT NULL auto_increment,
  `cmd` varchar(32) NOT NULL default 'start',
  `medscript` varchar(256) NOT NULL,
  `timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP,
  PRIMARY KEY  (`cid`)
) ENGINE=MyISAM AUTO_INCREMENT=222 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CS_data`
--

DROP TABLE IF EXISTS `CS_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_data` (
  `path` varchar(256) NOT NULL default '0',
  `size` varchar(20) NOT NULL default '0',
  `type` varchar(32) NOT NULL default '0',
  `time` varchar(20) NOT NULL,
  `ap` varchar(16) default NULL,
  `imei` varchar(64) NOT NULL,
  `uid` varchar(64) NOT NULL,
  `pid` varchar(16) NOT NULL,
  `qid` varchar(16) NOT NULL,
  `muid` varchar(16) NOT NULL,
  `review` varchar(512) NOT NULL,
  `arrivaltime` timestamp NOT NULL default CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `CS_env`
--

DROP TABLE IF EXISTS `CS_env`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_env` (
  `medkey` varchar(64) NOT NULL,
  `medvalue` varchar(1024) NOT NULL,
  `meddesc` varchar(256) NOT NULL,
  `timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CS_env`
--

LOCK TABLES `CS_env` WRITE;
/*!40000 ALTER TABLE `CS_env` DISABLE KEYS */;
INSERT INTO `CS_env` VALUES 
  ('BASE-URL-DATA','xxx.xxx.xxx.xxx/Medusa/medusa-cloud/tasktracker/','','2011-12-06 06:06:36'),
  ('SPC-HOST','xxx.xxx.xxx.xxx','','2011-12-06 06:07:31'),
  ('SPC-URI','/Medusa/medusa-cloud/tasktracker/web_tt_service.php','','2011-12-06 06:07:31'),
  ('CS-DBHOST','xxx.xxx.xxx.xxx','','2011-12-06 06:08:15'),
  ('CS-DBNAME','medusa','','2011-12-06 06:08:15'),
  ('HIT-HOST','xxx.xxx.xxx.xxx:80','','2011-12-06 06:08:46'),
  ('HIT-URI','/medusa_hit_server/MainServlet','','2011-12-06 06:08:46'),
  ('FAIL-SLEEP-INTERVAL','3','sleep duration (second) when a stage fails.','2011-12-06 06:10:45'),
  ('FAIL-RETRY-CNT','6','The number of retry when failed == # of SMS messages to the phone for one stage execution','2011-12-06 06:12:04'),
  ('WAIT-DURATION-FOR-STAGE','1200','Unit: sec. Whole wait time for one HIT or SPC request.','2011-12-06 06:14:22'),
  ('CHECK-PERIOD-FOR-STAGE-OUTPUT','5','check if there is an output from the HIT or SPC. repeat until WAIT-DURATION-FOR-STAGE time passes','2011-12-06 06:15:27');
/*!40000 ALTER TABLE `CS_env` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `CS_event`
--

DROP TABLE IF EXISTS `CS_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_event` (
  `pid` varchar(16) NOT NULL,
  `userid` varchar(64) NOT NULL,
  `action` varchar(64) NOT NULL,
  `qtype` varchar(64) NOT NULL,
  `qid` varchar(16) NOT NULL,
  `msg` varchar(1024) NOT NULL,
  `timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CS_globalvars`
--

DROP TABLE IF EXISTS `CS_globalvars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CS_globalvars` (
  `pid` varchar(64) NOT NULL,
  `envkey` varchar(64) NOT NULL,
  `envval` varchar(256) NOT NULL,
  `timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `medusalets`
--

DROP TABLE IF EXISTS `medusalets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `medusalets` (
  `Idx` int(11) NOT NULL auto_increment,
  `Name` varchar(64) NOT NULL,
  `Desc` varchar(256) NOT NULL,
  PRIMARY KEY  (`Idx`)
) ENGINE=MyISAM AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medusalets`
--

LOCK TABLES `medusalets` WRITE;
/*!40000 ALTER TABLE `medusalets` DISABLE KEYS */;
INSERT INTO `medusalets` VALUES (1,'medusalet_helloworld','hello world'),(2,'medusalet_mediagen','media generator: image/video'),(3,'medusalet_uploaddata','data uploader');
/*!40000 ALTER TABLE `medusalets` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2012-06-17  8:16:30
