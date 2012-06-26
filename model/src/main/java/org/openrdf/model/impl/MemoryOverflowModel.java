/*
 * Copyright (c) 2012, 3 Round Stones Inc. Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.openrdf.model.impl;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model implementation that stores in a {@link LinkedHashModel} until more than
 * 10KB statements are added and the estimated memory usage is more than the
 * amount of free memory available. Once the threshold is cross this
 * implementation seamlessly changes to a disk based {@link RepositoryModel}.
 * 
 * @author James Leigh
 * 
 */
public class MemoryOverflowModel extends AbstractModel {
	private static final long serialVersionUID = 4119844228099208169L;
	private static final Runtime RUNTIME = Runtime.getRuntime();
	private static final int LARGE_BLOCK = 10000;
	private final Logger logger = LoggerFactory
			.getLogger(MemoryOverflowModel.class);
	private LinkedHashModel memory;
	private Repository repository;
	private RepositoryConnection connection;
	private RepositoryModel disk;
	private long baseline = 0;
	private long maxBlockSize = 0;

	public MemoryOverflowModel() {
		memory = new LinkedHashModel();
	}

	public MemoryOverflowModel(Model model) {
		this(model.getNamespaces(), model.size());
		addAll(model);
	}

	public MemoryOverflowModel(int size) {
		memory = new LinkedHashModel(size);
	}

	public MemoryOverflowModel(Map<String, String> namespaces,
			Collection<? extends Statement> c) {
		this(namespaces, c.size());
		addAll(c);
	}

	public MemoryOverflowModel(Map<String, String> namespaces) {
		memory = new LinkedHashModel(namespaces);
	}

	public MemoryOverflowModel(Map<String, String> namespaces, int size) {
		memory = new LinkedHashModel(namespaces, size);
	}

	@Override
	public synchronized void closeIterator(Iterator<?> iter) {
		super.closeIterator(iter);
		if (disk == null) {
			memory.closeIterator(iter);
		} else {
			disk.closeIterator(iter);
		}
	}

	public synchronized Map<String, String> getNamespaces() {
		return memory.getNamespaces();
	}

	public synchronized String getNamespace(String prefix) {
		return memory.getNamespace(prefix);
	}

	public synchronized String setNamespace(String prefix, String name) {
		return memory.setNamespace(prefix, name);
	}

	public synchronized String removeNamespace(String prefix) {
		return memory.removeNamespace(prefix);
	}

	public boolean contains(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		return getDelegate().contains(subj, pred, obj, contexts);
	}

	public boolean add(Resource subj, URI pred, Value obj, Resource... contexts) {
		checkMemoryOverflow();
		return getDelegate().add(subj, pred, obj, contexts);
	}

	public boolean remove(Resource subj, URI pred, Value obj,
			Resource... contexts) {
		return getDelegate().remove(subj, pred, obj, contexts);
	}

	public int size() {
		return getDelegate().size();
	}

	public Iterator<Statement> iterator() {
		return getDelegate().iterator();
	}

	public boolean clear(Resource... contexts) {
		return getDelegate().clear(contexts);
	}

	public Model filter(final Resource subj, final URI pred, final Value obj,
			final Resource... contexts) {
		return new FilteredModel(this, subj, pred, obj, contexts) {
			private static final long serialVersionUID = -475666402618133101L;

			@Override
			public int size() {
				return getDelegate().filter(subj, pred, obj, contexts).size();
			}

			@Override
			public Iterator<Statement> iterator() {
				return getDelegate().filter(subj, pred, obj, contexts)
						.iterator();
			}
		};
	}

	@Override
	protected synchronized void removeIteration(Iterator<Statement> iter,
			Resource subj, URI pred, Value obj, Resource... contexts) {
		if (disk == null) {
			memory.removeIteration(iter, subj, pred, obj, contexts);
		} else {
			disk.removeIteration(iter, subj, pred, obj, contexts);
		}
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		// Write out any hidden serialization magic
		s.defaultWriteObject();
		// Write in size
		Model delegate = getDelegate();
		s.writeInt(delegate.size());
		// Write in all elements
		for (Statement st : delegate) {
			Resource subj = st.getSubject();
			URI pred = st.getPredicate();
			Value obj = st.getObject();
			Resource ctx = st.getContext();
			s.writeObject(new ContextStatementImpl(subj, pred, obj, ctx));
		}
	}

	private void readObject(ObjectInputStream s) throws IOException,
			ClassNotFoundException {
		// Read in any hidden serialization magic
		s.defaultReadObject();
		// Read in size
		int size = s.readInt();
		// Read in all elements
		for (int i = 0; i < size; i++) {
			add((Statement) s.readObject());
		}
	}

	private synchronized Model getDelegate() {
		if (disk == null)
			return memory;
		return disk;
	}

	private synchronized void checkMemoryOverflow() {
		if (disk == null) {
			int size = size();
			if (size >= LARGE_BLOCK && size % LARGE_BLOCK == 0) {
				long totalMemory = RUNTIME.totalMemory();
				long freeMemory = RUNTIME.freeMemory();
				long used = totalMemory - freeMemory;
				if (baseline > 0) {
					long blockSize = used - baseline;
					if (blockSize > maxBlockSize) {
						maxBlockSize = blockSize;
					}
					if (freeMemory < size / LARGE_BLOCK * maxBlockSize) {
						// may not be enough free memory for another
						overflowToDisk();
					}
				}
				baseline = used;
			}
		}
	}

	private synchronized void overflowToDisk() {
		try {
			assert disk == null;
			repository = createRepository();
			connection = repository.getConnection();
			connection.setAutoCommit(false);
			disk = new RepositoryModel(connection) {

				@Override
				protected void finalize() throws Throwable {
					if (disk == this) {
						try {
							if (connection != null) {
								connection.commit();
								connection.close();
							}
							repository.shutDown();
						} catch (RepositoryException e) {
							logger.error(e.toString(), e);
						} finally {
							FileUtil.deltree(repository.getDataDir());
							repository = null;
							connection = null;
							disk = null;
						}
					}
					super.finalize();
				}
			};
			disk.addAll(memory);
			memory = new LinkedHashModel(memory.getNamespaces());
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}

	private Repository createRepository() throws IOException,
			RepositoryException {
		File dir = createTempDir("model");
		SailRepository repo = new SailRepository(new NativeStore(dir, "spoc,pocs,oscp,cspo"));
		repo.initialize();
		return repo;
	}

	private File createTempDir(String name) throws IOException {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		File tmp = File.createTempFile(name, "");
		tmp.delete();
		tmp.mkdir();
		return tmp;
	}

}
