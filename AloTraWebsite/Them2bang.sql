CREATE TABLE ChatRooms ( ---------------CHẠY CÁI NÀY
    Id BIGINT PRIMARY KEY IDENTITY(1,1),
    UserId BIGINT NOT NULL,
    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
    Status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ClosedAt DATETIME NULL,
    FOREIGN KEY (CustomerId) REFERENCES Users(Id) ON DELETE CASCADE
);

-- Tạo bảng ChatMessages
CREATE TABLE ChatMessages (----------------------CHẠY CÁI NÀY
    Id BIGINT PRIMARY KEY IDENTITY(1,1),
    RoomId BIGINT NOT NULL,
    SenderId BIGINT NOT NULL,
    Content NVARCHAR(MAX) NOT NULL,
    Timestamp DATETIME NOT NULL DEFAULT GETDATE(),
    IsRead BIT NOT NULL DEFAULT 0,
    SenderType NVARCHAR(50) NOT NULL,
    FOREIGN KEY (RoomId) REFERENCES ChatRooms(Id) ON DELETE CASCADE,
    FOREIGN KEY (SenderId) REFERENCES Users(Id) ON DELETE NO ACTION
);



-- Tạo index để tối ưu query----------------------CHẠY CÁI NÀY
CREATE INDEX idx_chatroom_customer ON ChatRooms(CustomerId);
CREATE INDEX idx_chatmessage_room ON ChatMessages(RoomId);
CREATE INDEX idx_chatmessage_sender ON ChatMessages(SenderId);
CREATE INDEX idx_chatmessage_unread ON ChatMessages(RoomId, IsRead);
CREATE INDEX idx_users_branchid ON Users([BranchId]);


ALTER TABLE ChatRooms 
DROP CONSTRAINT FK__ChatRooms__Custo__6442E2C9;

EXEC sp_rename 'ChatRooms.CustomerId', 'UserId', 'COLUMN';

ALTER TABLE ChatRooms
ADD CONSTRAINT FK_ChatRooms_Users 

FOREIGN KEY (UserId) REFERENCES Users(Id) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_chatroom_customer ON ChatRooms;

CREATE INDEX idx_chatroom_user ON ChatRooms(UserId);
SELECT 
  fk.name                AS FK_Name,
  sch.name               AS SchemaName,
  pt.name                AS ParentTable,
  pc.name                AS ParentColumn,
  rt.name                AS RefTable,
  rc.name                AS RefColumn,
  fk.delete_referential_action_desc AS OnDelete,
  fk.update_referential_action_desc AS OnUpdate
FROM sys.foreign_keys fk
JOIN sys.schemas sch               ON sch.schema_id = fk.schema_id
JOIN sys.foreign_key_columns fkc   ON fkc.constraint_object_id = fk.object_id
JOIN sys.tables pt                 ON pt.object_id = fk.parent_object_id
JOIN sys.columns pc                ON pc.object_id = fkc.parent_object_id
                                   AND pc.column_id = fkc.parent_column_id
JOIN sys.tables rt                 ON rt.object_id = fkc.referenced_object_id
JOIN sys.columns rc                ON rc.object_id = fkc.referenced_object_id
                                   AND rc.column_id = fkc.referenced_column_id
WHERE fk.parent_object_id = OBJECT_ID('dbo.ChatRooms');


ALTER TABLE Users ADD [BranchId] BIGINT NULL;

ALTER TABLE Users ADD CONSTRAINT fk_users_branchid 
    FOREIGN KEY ([BranchId]) REFERENCES Branches([Id]);

	SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'BranchId';


SELECT Id, Email, FullName, BranchId FROM Users WHERE Email = 'manager.td@alotra.com';