import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './student.reducer';

export const StudentDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const studentEntity = useAppSelector(state => state.gateway.student.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="studentDetailsHeading">
          <Translate contentKey="gatewayApp.student.detail.title">Student</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{studentEntity.id}</dd>
          <dt>
            <span id="nickname">
              <Translate contentKey="gatewayApp.student.nickname">Nickname</Translate>
            </span>
          </dt>
          <dd>{studentEntity.nickname}</dd>
          <dt>
            <span id="age">
              <Translate contentKey="gatewayApp.student.age">Age</Translate>
            </span>
          </dt>
          <dd>{studentEntity.age}</dd>
          <dt>
            <span id="avatarStyle">
              <Translate contentKey="gatewayApp.student.avatarStyle">Avatar Style</Translate>
            </span>
          </dt>
          <dd>{studentEntity.avatarStyle}</dd>
          <dt>
            <Translate contentKey="gatewayApp.student.parent">Parent</Translate>
          </dt>
          <dd>{studentEntity.parent ? studentEntity.parent.id : ''}</dd>
        </dl>
        <Button tag={Link} to="/student" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/student/${studentEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default StudentDetail;
