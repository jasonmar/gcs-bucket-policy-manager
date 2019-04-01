/* Copyright 2019 Google LLC. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package com.google.cloud.util.bpm

import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import java.{lang, util}

import com.google.api.gax.paging.Page
import com.google.cloud.storage._
import com.google.cloud.{Policy, ReadChannel, WriteChannel}

class NoOpStorage extends Storage {
  private var getOps: Long = 0
  private var setOps: Long = 0

  def getCount: Long = getOps
  def setCount: Long = setOps

  override def getIamPolicy(bucket: String, options: Storage.BucketSourceOption*): Policy = {
    getOps += 1
    Policy.newBuilder().build()
  }

  override def setIamPolicy(bucket: String, policy: Policy, options: Storage.BucketSourceOption*): Policy = {
    setOps += 1
    policy
  }

  override def create(bucketInfo: BucketInfo, options: Storage.BucketTargetOption*): Bucket = { throw new NotImplementedError() }

  override def create(blobInfo: BlobInfo, options: Storage.BlobTargetOption*): Blob = { throw new NotImplementedError() }

  override def create(blobInfo: BlobInfo, content: Array[Byte], options: Storage.BlobTargetOption*): Blob = { throw new NotImplementedError() }

  override def create(blobInfo: BlobInfo, content: Array[Byte], offset: Int, length: Int, options: Storage.BlobTargetOption*): Blob = { throw new NotImplementedError() }

  override def create(blobInfo: BlobInfo, content: InputStream, options: Storage.BlobWriteOption*): Blob = { throw new NotImplementedError() }

  override def get(bucket: String, options: Storage.BucketGetOption*): Bucket = { throw new NotImplementedError() }

  override def lockRetentionPolicy(bucket: BucketInfo, options: Storage.BucketTargetOption*): Bucket = { throw new NotImplementedError() }

  override def get(bucket: String, blob: String, options: Storage.BlobGetOption*): Blob = { throw new NotImplementedError() }

  override def get(blob: BlobId, options: Storage.BlobGetOption*): Blob = { throw new NotImplementedError() }

  override def get(blob: BlobId): Blob = { throw new NotImplementedError() }

  override def list(options: Storage.BucketListOption*): Page[Bucket] = { throw new NotImplementedError() }

  override def list(bucket: String, options: Storage.BlobListOption*): Page[Blob] = { throw new NotImplementedError() }

  override def update(bucketInfo: BucketInfo, options: Storage.BucketTargetOption*): Bucket = { throw new NotImplementedError() }

  override def update(blobInfo: BlobInfo, options: Storage.BlobTargetOption*): Blob = { throw new NotImplementedError() }

  override def update(blobInfo: BlobInfo): Blob = { throw new NotImplementedError() }

  override def delete(bucket: String, options: Storage.BucketSourceOption*): Boolean = { throw new NotImplementedError() }

  override def delete(bucket: String, blob: String, options: Storage.BlobSourceOption*): Boolean = { throw new NotImplementedError() }

  override def delete(blob: BlobId, options: Storage.BlobSourceOption*): Boolean = { throw new NotImplementedError() }

  override def delete(blob: BlobId): Boolean = { throw new NotImplementedError() }

  override def compose(composeRequest: Storage.ComposeRequest): Blob = { throw new NotImplementedError() }

  override def copy(copyRequest: Storage.CopyRequest): CopyWriter = { throw new NotImplementedError() }

  override def readAllBytes(bucket: String, blob: String, options: Storage.BlobSourceOption*): Array[Byte] = { throw new NotImplementedError() }

  override def readAllBytes(blob: BlobId, options: Storage.BlobSourceOption*): Array[Byte] = { throw new NotImplementedError() }

  override def batch(): StorageBatch = { throw new NotImplementedError() }

  override def reader(bucket: String, blob: String, options: Storage.BlobSourceOption*): ReadChannel = { throw new NotImplementedError() }

  override def reader(blob: BlobId, options: Storage.BlobSourceOption*): ReadChannel = { throw new NotImplementedError() }

  override def writer(blobInfo: BlobInfo, options: Storage.BlobWriteOption*): WriteChannel = { throw new NotImplementedError() }

  override def signUrl(blobInfo: BlobInfo, duration: Long, unit: TimeUnit, options: Storage.SignUrlOption*): URL = { throw new NotImplementedError() }

  override def get(blobIds: BlobId*): util.List[Blob] = { throw new NotImplementedError() }

  override def get(blobIds: lang.Iterable[BlobId]): util.List[Blob] = { throw new NotImplementedError() }

  override def update(blobInfos: BlobInfo*): util.List[Blob] = { throw new NotImplementedError() }

  override def update(blobInfos: lang.Iterable[BlobInfo]): util.List[Blob] = { throw new NotImplementedError() }

  override def delete(blobIds: BlobId*): util.List[lang.Boolean] = { throw new NotImplementedError() }

  override def delete(blobIds: lang.Iterable[BlobId]): util.List[lang.Boolean] = { throw new NotImplementedError() }

  override def getAcl(bucket: String, entity: Acl.Entity, options: Storage.BucketSourceOption*): Acl = { throw new NotImplementedError() }

  override def getAcl(bucket: String, entity: Acl.Entity): Acl = { throw new NotImplementedError() }

  override def deleteAcl(bucket: String, entity: Acl.Entity, options: Storage.BucketSourceOption*): Boolean = { throw new NotImplementedError() }

  override def deleteAcl(bucket: String, entity: Acl.Entity): Boolean = { throw new NotImplementedError() }

  override def createAcl(bucket: String, acl: Acl, options: Storage.BucketSourceOption*): Acl = { throw new NotImplementedError() }

  override def createAcl(bucket: String, acl: Acl): Acl = { throw new NotImplementedError() }

  override def updateAcl(bucket: String, acl: Acl, options: Storage.BucketSourceOption*): Acl = { throw new NotImplementedError() }

  override def updateAcl(bucket: String, acl: Acl): Acl = { throw new NotImplementedError() }

  override def listAcls(bucket: String, options: Storage.BucketSourceOption*): util.List[Acl] = { throw new NotImplementedError() }

  override def listAcls(bucket: String): util.List[Acl] = { throw new NotImplementedError() }

  override def getDefaultAcl(bucket: String, entity: Acl.Entity): Acl = { throw new NotImplementedError() }

  override def deleteDefaultAcl(bucket: String, entity: Acl.Entity): Boolean = { throw new NotImplementedError() }

  override def createDefaultAcl(bucket: String, acl: Acl): Acl = { throw new NotImplementedError() }

  override def updateDefaultAcl(bucket: String, acl: Acl): Acl = { throw new NotImplementedError() }

  override def listDefaultAcls(bucket: String): util.List[Acl] = { throw new NotImplementedError() }

  override def getAcl(blob: BlobId, entity: Acl.Entity): Acl = { throw new NotImplementedError() }

  override def deleteAcl(blob: BlobId, entity: Acl.Entity): Boolean = { throw new NotImplementedError() }

  override def createAcl(blob: BlobId, acl: Acl): Acl = { throw new NotImplementedError() }

  override def updateAcl(blob: BlobId, acl: Acl): Acl = { throw new NotImplementedError() }

  override def listAcls(blob: BlobId): util.List[Acl] = { throw new NotImplementedError() }

  override def testIamPermissions(bucket: String, permissions: util.List[String], options: Storage.BucketSourceOption*): util.List[lang.Boolean] = { throw new NotImplementedError() }

  override def getServiceAccount(projectId: String): ServiceAccount = { throw new NotImplementedError() }

  override def getOptions: StorageOptions = { throw new NotImplementedError() }
}
