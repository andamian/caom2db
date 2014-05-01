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

package ca.nrc.cadc.caom2.persistence;

import ca.nrc.cadc.caom2.access.ObservationMetaReadAccess;
import ca.nrc.cadc.caom2.access.ReadAccess;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public abstract class AbstractDatabaseReadAccessDAOTest
{
    protected static Logger log;

    boolean deletionTrack;
    DatabaseReadAccessDAO dao;
    DatabaseTransactionManager txnManager;

    protected Class[] entityClasses;

    protected AbstractDatabaseReadAccessDAOTest(Class genClass, String server, String database, String schema, boolean deletionTrack)
        throws Exception
    {
        this.deletionTrack = deletionTrack;
        try
        {
            Map<String,Object> config = new TreeMap<String,Object>();
            config.put("server", server);
            config.put("database", database);
            config.put("schema", schema);
            config.put(SQLGenerator.class.getName(), genClass);
            this.dao = new DatabaseReadAccessDAO();
            dao.setConfig(config);
            this.txnManager = new DatabaseTransactionManager(dao.getDataSource());
        }
        catch(Exception ex)
        {
            // make sure it gets fully dumped
            log.error("setup DataSource failed", ex);
            throw ex;
        }
    }

    @Before
    public void setup()
        throws Exception
    {
        log.debug("clearing old tables...");
        SQLGenerator gen = dao.getSQLGenerator();
        DataSource ds = dao.getDataSource();
        for (Class c : entityClasses)
        {
            String cn = c.getSimpleName();
            String s = gen.getTable(c);

            String sql = "delete from " + s;
            log.debug("setup: " + sql);
            ds.getConnection().createStatement().execute(sql);
            if (deletionTrack)
            {
                sql = sql.replace(cn, "Deleted"+cn);
                log.debug("setup: " + sql);
                ds.getConnection().createStatement().execute(sql);
            }
        }
        log.debug("clearing old tables... OK");
    }

    @Test
    public void testGet()
    {
        try
        {
            UUID id = UUID.randomUUID();
            for (int i=0; i<entityClasses.length; i++)
            {
                ReadAccess ra = dao.get(entityClasses[i], id);
                Assert.assertNull(entityClasses[i].getSimpleName(), ra);
            }
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    protected void doPutGetDelete(ReadAccess expected)
        throws Exception
    {
        String s = expected.getClass().getSimpleName();
        dao.put(expected);
        ReadAccess actual = dao.get(expected.getClass(), expected.getID());
        Assert.assertNotNull(s, actual);
        Assert.assertEquals(s+".assetID", expected.getAssetID(), actual.getAssetID());
        Assert.assertEquals(s+".groupID", expected.getGroupID(), actual.getGroupID());
        testEqual(s+".lastModified", expected.getLastModified(), actual.getLastModified());
        //Assert.assertEquals(s+".getStateCode", expected.getStateCode(), actual.getStateCode());

        dao.delete(expected.getClass(), expected.getID());
        actual = dao.get(expected.getClass(), expected.getID());
        Assert.assertNull(actual);
    }

    @Test
    public void testPutGetDelete()
    {
        try
        {
            Long assetID = new Long(666L);
            Long groupID =  new Long(777L);
            ReadAccess expected;

            for (Class c : entityClasses)
            {
                Constructor ctor = c.getConstructor(Long.class, Long.class);
                expected = (ReadAccess) ctor.newInstance(assetID, groupID);
                doPutGetDelete(expected);
            }
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    // for comparing lastModified: Sybase isn't reliable to ms accuracy when using UTC
    protected void testEqual(String s, Date expected, Date actual)
    {
        log.debug("testEqual(Date,Date): " + expected.getTime() + " vs " + actual.getTime());
        if (expected == null)
        {
            Assert.assertNull(s, actual);
            return;
        }

        Assert.assertTrue(s, Math.abs(expected.getTime() - actual.getTime()) < 3L);
    }
}