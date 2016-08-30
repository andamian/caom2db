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

import ca.nrc.cadc.caom2.Artifact;
import ca.nrc.cadc.caom2.Chunk;
import ca.nrc.cadc.caom2.Observation;
import ca.nrc.cadc.caom2.Part;
import ca.nrc.cadc.caom2.Plane;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Class that uses PartialRowMapper(s) to construct a List<Observation> out of a ResultSet.
 * 
 * @author pdowler
 */
public class BaseObservationExtractor implements ObservationExtractor
{
    private static Logger log = Logger.getLogger(BaseObservationExtractor.class);
    
    private PartialRowMapper<Observation> obsMapper;
    private PartialRowMapper<Plane> planeMapper;
    private PartialRowMapper<Artifact> artifactMapper;
    private PartialRowMapper<Part> partMapper;
    private PartialRowMapper<Chunk> chunkMapper;
    
    public BaseObservationExtractor(BaseSQLGenerator gen)
    {
        this.obsMapper = gen.getObservationMapper();
        this.planeMapper = gen.getPlaneMapper();
        this.artifactMapper = gen.getArtifactMapper();
        this.partMapper = gen.getPartMapper();
        this.chunkMapper = gen.getChunkMapper();
    }

    public Object extractData(ResultSet rs) 
        throws SQLException
    {
        return extractObservations(rs);
    }

    public List<Observation> extractObservations(ResultSet rs)
        throws SQLException
    {
        int ncol = rs.getMetaData().getColumnCount();
        log.debug("extractData: ncol=" +  ncol);
        List<Observation> ret = new ArrayList<Observation>();
        Observation curObs = null;
        Plane curPlane = null;
        Artifact curArtifact = null;
        Part curPart = null;
        Chunk curChunk = null;
        int row = 0;
        while ( rs.next() )
        {
            row++;
            int col = 1;
            log.debug("mapping Observation at column " + col);
            Observation obs = obsMapper.mapRow(rs, row, col);
            col += obsMapper.getColumnCount();
            if ( curObs == null || !curObs.getID().equals(obs.getID()) )
            {
                if (curObs != null) // found first row of next observation
                    log.debug("END observation: " + curObs.getID());
                
                curObs = obs;
                ret.add(curObs);
                log.debug("START observation: " + curObs.getID());
            }
            // else: obs content repeated due to join -- ignore it
            
            if (ncol > col) // more columns==depth>1: planes
            {
                log.debug("mapping Plane at column " + col);
                Plane p = planeMapper.mapRow(rs, row, col);
                col += planeMapper.getColumnCount();
                if (p != null)
                {
                    if (curPlane == null || !curPlane.getID().equals(p.getID()))
                    {
                        if (curPlane != null) // found first row of next plane in same obs
                            log.debug("END plane: " + curPlane.getID());
                        curPlane = p;
                        curObs.getPlanes().add(p);
                        log.debug("START plane: " + curPlane.getID());
                    }
                    //else:  plane content repeated due to join -- ignore it
                }
                else
                {
                    log.debug("observation: " + curObs.getID() + ": no planes");
                    curPlane = null;
                }
            }
            if (curPlane != null && ncol > col) // more columns==depth>2: artifacts
            {
                log.debug("mapping Artifact at column " + col);
                Artifact a = artifactMapper.mapRow(rs, row, col);
                col += artifactMapper.getColumnCount();
                if (a != null)
                {
                    if (curArtifact == null || !curArtifact.getID().equals(a.getID()))
                    {
                        if (curArtifact != null) // found first row of next artifact in same plane
                            log.debug("END artifact: " + curArtifact.getID());
                        curArtifact = a;
                        curPlane.getArtifacts().add(curArtifact);
                        log.debug("START artifact: " + curArtifact.getID());
                    }
                    //else: artifact content repeated due to join -- ignore it
                }
                else
                {
                    log.debug("plane: " + curPlane.getID() + ": no artifacts");
                    curArtifact = null;
                }
            }
            if (curArtifact != null && ncol > col) // more columns==depth>3: parts
            {
                log.debug("mapping Part at column " + col);
                Part p = partMapper.mapRow(rs, row, col);
                col += partMapper.getColumnCount();
                if (p != null)
                {
                    if (curPart == null || !curPart.getID().equals(p.getID()))
                    {
                        if (curPart != null) // found first row of next artifact in same plane
                            log.debug("END part: " + curPart.getID());
                        curPart = p;
                        curArtifact.getParts().add(curPart);
                        log.debug("START part: " + curPart.getID());
                    }
                    //else: artifact content repeated due to join -- ignore it
                }
                else
                {
                    log.debug("artifact: " + curArtifact.getID() + ": no parts");
                    curPart = null;
                }
            }
            if (curPart != null && ncol > col) // more columns==depth>4: chunks
            {
                log.debug("mapping Chunk at column " + col);
                Chunk c = chunkMapper.mapRow(rs, row, col);
                col += chunkMapper.getColumnCount();
                if (c != null)
                {
                    if (curChunk == null || !curChunk.getID().equals(c.getID()))
                    {
                        if (curChunk != null)
                            log.debug("END part: " + curChunk.getID());
                        curChunk = c;
                        curPart.getChunks().add(curChunk);
                        log.debug("START chunk: " + curChunk.getID());
                    }
                    //else: artifact content repeated due to join -- ignore it
                }
                else
                {
                    log.debug("part: " + curPart.getID() + ": no chunks");
                    curChunk = null;
                }
            }
        }
       
        return ret;
    }
}