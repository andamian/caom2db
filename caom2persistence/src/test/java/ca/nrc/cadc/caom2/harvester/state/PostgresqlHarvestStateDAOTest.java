/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.caom2.harvester.state;

import ca.nrc.cadc.caom2.persistence.UtilTest;
import ca.nrc.cadc.caom2.version.InitDatabase;
import ca.nrc.cadc.db.ConnectionConfig;
import ca.nrc.cadc.db.DBConfig;
import ca.nrc.cadc.db.DBUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class PostgresqlHarvestStateDAOTest
{
    private static final Logger log = Logger.getLogger(PostgresqlHarvestStateDAOTest.class);

    static String schema = "caom2";

    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.caom2.harvester", Level.INFO);

        String testSchema = UtilTest.getTestSchema();
        if (testSchema != null)
        {
            schema = testSchema;
        }
    }

    DataSource dataSource;
    String database;

    public PostgresqlHarvestStateDAOTest()
        throws Exception
    {
        this.database = "cadctest";
        DBConfig dbrc = new DBConfig();
        ConnectionConfig cc = dbrc.getConnectionConfig("CAOM2_PG_TEST", database);
        this.dataSource = DBUtil.getDataSource(cc);

        InitDatabase init = new InitDatabase(dataSource, "cadctest", schema);
        init.doInit();

        String sql = "DELETE FROM " + database + "." + schema + ".HarvestState";
        log.info("cleanup: " + sql);
        dataSource.getConnection().createStatement().execute(sql);
    }

    //@Test
    public void testTemplate()
    {
        try
        {

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testGet()
    {
        try
        {
            HarvestStateDAO dao = new PostgresqlHarvestStateDAO(dataSource, database, schema);
            HarvestState s = dao.get("testGet", Integer.class.getName());
            Assert.assertNotNull(s);
            Assert.assertEquals("testGet", s.source);
            Assert.assertEquals(Integer.class.getName(), s.cname);
            Assert.assertNull(s.curLastModified);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInsertID()
    {
        try
        {
            HarvestStateDAO dao = new PostgresqlHarvestStateDAO(dataSource, database, schema);
            HarvestState s = dao.get("testInsertID", Integer.class.getName());
            Assert.assertNotNull(s);
            Assert.assertNull(s.curLastModified);

            s.curID = UUID.randomUUID();
            dao.put(s);

            HarvestState s2 = dao.get("testInsertID", Integer.class.getName());
            Assert.assertNotNull(s2);
            Assert.assertNull(s2.curLastModified);
            Assert.assertEquals(s.id, s2.id);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInsertDate()
    {
        try
        {
            HarvestStateDAO dao = new PostgresqlHarvestStateDAO(dataSource, database, schema);
            HarvestState s = dao.get("testInsertDate", Integer.class.getName());
            Assert.assertEquals("testInsertDate", s.source);
            Assert.assertNotNull(s);
            Assert.assertNull(s.curLastModified);

            s.curLastModified = new Date();
            dao.put(s);

            HarvestState s2 = dao.get("testInsertDate", Integer.class.getName());
            Assert.assertNotNull(s2);
            Assert.assertEquals(s.curLastModified, s2.curLastModified);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testUpdateID()
    {
        try
        {
            HarvestStateDAO dao = new PostgresqlHarvestStateDAO(dataSource, database, schema);
            HarvestState s = dao.get("testUpdateID", Integer.class.getName());
            Assert.assertNotNull(s);
            Assert.assertNull(s.curLastModified);

            s.curID = UUID.randomUUID();
            dao.put(s);

            HarvestState s2 = dao.get("testUpdateID", Integer.class.getName());
            Assert.assertNotNull(s2);
            Assert.assertNull(s2.curLastModified);
            Assert.assertEquals(s.id, s2.id);

            s.curID = UUID.randomUUID();
            dao.put(s);

            HarvestState s3 = dao.get("testUpdateID", Integer.class.getName());
            Assert.assertNotNull(s3);
            Assert.assertNull(s3.curLastModified);
            Assert.assertEquals(s.id, s3.id);

        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testUpdateDate()
    {
        try
        {
            HarvestStateDAO dao = new PostgresqlHarvestStateDAO(dataSource, database, schema);
            HarvestState s = dao.get("testUpdateDate", Integer.class.getName());
            Assert.assertEquals("testUpdateDate", s.source);
            Assert.assertNotNull(s);
            Assert.assertNull(s.curLastModified);

            long t = System.currentTimeMillis();
            s.curLastModified = new Date(t);
            dao.put(s);

            HarvestState s2 = dao.get("testUpdateDate", Integer.class.getName());
            Assert.assertNotNull(s2);
            Assert.assertEquals(s.curLastModified, s2.curLastModified);

            s.curLastModified = new Date(t + 10L);
            dao.put(s);

            HarvestState s3 = dao.get("testUpdateDate", Integer.class.getName());
            Assert.assertNotNull(s3);
            Assert.assertEquals(s.curLastModified, s3.curLastModified);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
